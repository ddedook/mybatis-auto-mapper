package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.core.MethodParameter;

import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class MapperAnnotationAutoBuilder {
  
  private final Set<Class<? extends Annotation>> sqlAnnotationTypes = new HashSet<>();
  private final Set<Class<? extends Annotation>> sqlProviderAnnotationTypes = new HashSet<>();
  
  private Configuration configuration;
  private MapperBuilderAssistant assistant;
  private Class<?> type;
  private EntityExpress entityExpress;
  
  public MapperAnnotationAutoBuilder(Configuration configuration, Class<?> type) {
    String resource = type.getName().replace('.', '/') + ".java (best guess)";
    this.assistant = new MapperBuilderAssistant(configuration, resource);
    this.configuration = configuration;
    this.type = type;
    
    sqlAnnotationTypes.add(Select.class);
    sqlAnnotationTypes.add(Insert.class);
    sqlAnnotationTypes.add(Update.class);
    sqlAnnotationTypes.add(Delete.class);
    
    sqlProviderAnnotationTypes.add(SelectProvider.class);
    sqlProviderAnnotationTypes.add(InsertProvider.class);
    sqlProviderAnnotationTypes.add(UpdateProvider.class);
    sqlProviderAnnotationTypes.add(DeleteProvider.class);
  }
  
  public void parse() {
    String resource = type.toString();
    if (!configuration.isResourceLoaded(resource)) {
      this.entityExpress = new EntityExpress(this.type);
      loadXmlResource();
      configuration.addLoadedResource(resource);
      assistant.setCurrentNamespace(type.getName());
      parseCache();
      parseCacheRef();
      Method[] methods = type.getMethods();
      for (Method method : methods) {
        try {
          if (!method.isBridge()) {
            parseStatement(method);
          }
        } catch (IncompleteElementException e) {
          // ignore
        }
      }
    }
  }
  
  void parseStatement(Method method) {
    Class<?> parameterTypeClass = getParameterType(method);
    LanguageDriver languageDriver = getLanguageDriver(method);
    SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);
    if (sqlSource != null) {
      Options options = method.getAnnotation(Options.class);
      final String mappedStatementId = type.getName() + "." + method.getName();
      Integer fetchSize = null;
      Integer timeout = null;
      StatementType statementType = StatementType.PREPARED;
      ResultSetType resultSetType = ResultSetType.FORWARD_ONLY;
      SqlCommandType sqlCommandType = getSqlCommandType(method);
      boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
      boolean flushCache = !isSelect;
      boolean useCache = isSelect;
      
      KeyGenerator keyGenerator = null;
      String keyProperty = "id";
      String keyColumn = null;
      if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
        // first check for SelectKey annotation - that overrides everything else
        SelectKey selectKey = method.getAnnotation(SelectKey.class);
        if (selectKey != null) {
          keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method), languageDriver);
          keyProperty = selectKey.keyProperty();
        } else if (options == null) {
          if (sqlCommandType == SqlCommandType.INSERT) {
            keyGenerator = keyGeneratorFromEntity();
          }
          if (keyGenerator == null) {
            keyGenerator = configuration.isUseGeneratedKeys() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
          }
        } else {
          keyGenerator = options.useGeneratedKeys() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
          keyProperty = options.keyProperty();
          keyColumn = options.keyColumn();
        }
      } else {
        keyGenerator = new NoKeyGenerator();
      }
      
      if (options != null) {
        if (Options.FlushCachePolicy.TRUE.equals(options.flushCache())) {
          flushCache = true;
        } else if (Options.FlushCachePolicy.FALSE.equals(options.flushCache())) {
          flushCache = false;
        }
        useCache = options.useCache();
        fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null; //issue #348
        timeout = options.timeout() > -1 ? options.timeout() : null;
        statementType = options.statementType();
        resultSetType = options.resultSetType();
      }
      
      String resultMapId = null;
      ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
      if (resultMapAnnotation != null) {
        String[] resultMaps = resultMapAnnotation.value();
        StringBuilder sb = new StringBuilder();
        for (String resultMap : resultMaps) {
          if (sb.length() > 0) {
            sb.append(",");
          }
          sb.append(resultMap);
        }
        resultMapId = sb.toString();
      } else if (isSelect) {
        resultMapId = parseResultMap(method);
      }
      
      assistant.addMappedStatement(
        mappedStatementId,
        sqlSource,
        statementType,
        sqlCommandType,
        fetchSize,
        timeout,
        // ParameterMapID
        null,
        parameterTypeClass,
        resultMapId,
        getReturnType(method),
        resultSetType,
        flushCache,
        useCache,
        false,
        keyGenerator,
        keyProperty,
        keyColumn,
        // DatabaseID
        null,
        languageDriver,
        // ResultSets
        options != null ? nullOrEmpty(options.resultSets()) : null);
    }
  }
  
  private String parseResultMap(Method method) {
    String resultMapId = type.getName() + "." + method.getName() + "-Inline";
    Class<?> returnType = getReturnType(method);
    if (returnType.getAnnotation(Table.class) != null) {
      EntityExpress entityExpress = new EntityExpress(returnType);
      Map<String, String> columnMap = entityExpress.getColumnMap();
      Map<String, Class<?>> typeMap = entityExpress.getColumnTypeMap();
      List<ResultMapping> resultMappings = new ArrayList<>();
      columnMap.forEach((prop, column) ->
        resultMappings.add(new ResultMapping.Builder(
          configuration,
          prop,
          column,
          typeMap.get(prop)
        ).build()));
      org.apache.ibatis.mapping.ResultMap.Builder builder =
        new org.apache.ibatis.mapping.ResultMap.Builder(configuration, resultMapId, returnType, resultMappings);
      configuration.addResultMap(builder.build());
    }
    return resultMapId;
  }
  
  private KeyGenerator keyGeneratorFromEntity() {
    GeneratedValue generatedValue = entityExpress.getGeneratedValue();
    if (generatedValue != null) {
      String generator = generatedValue.generator();
      if (generator.equals("uuid") || generator.isEmpty()) {
        return new UuidGenerator();
      }
    }
    return null;
  }
  
  private Class<?> getParameterType(Method method) {
    Class<?> parameterType = null;
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (Class<?> type : parameterTypes) {
      if (!RowBounds.class.isAssignableFrom(type) && !ResultHandler.class.isAssignableFrom(type)) {
        if (parameterType == null) {
          parameterType = type;
        } else {
          parameterType = MapperMethod.ParamMap.class;
        }
      }
    }
    return parameterType;
  }
  
  private SqlCommandType getSqlCommandType(Method method) {
    Class<? extends Annotation> type = getSqlAnnotationType(method);
    if (type == null) {
      type = getSqlProviderAnnotationType(method);
      if (type == null) {
        String name = method.getName();
        if (name.startsWith("select") || name.startsWith("find")) {
          type = Select.class;
        } else if (name.startsWith("update")) {
          type = Update.class;
        } else if (name.startsWith("delete")) {
          type = Delete.class;
        } else if (name.startsWith("insert")) {
          type = Insert.class;
        } else {
          return SqlCommandType.UNKNOWN;
        }
      } else {
        if (type == SelectProvider.class) {
          type = Select.class;
        } else if (type == InsertProvider.class) {
          type = Insert.class;
        } else if (type == UpdateProvider.class) {
          type = Update.class;
        } else if (type == DeleteProvider.class) {
          type = Delete.class;
        }
      }
    }
    return SqlCommandType.valueOf(type.getSimpleName().toUpperCase(Locale.ENGLISH));
  }
  
  private Class<? extends Annotation> getSqlAnnotationType(Method method) {
    return chooseAnnotationType(method, sqlAnnotationTypes);
  }
  
  private Class<? extends Annotation> getSqlProviderAnnotationType(Method method) {
    return chooseAnnotationType(method, sqlProviderAnnotationTypes);
  }
  
  private Class<? extends Annotation> chooseAnnotationType(Method method, Set<Class<? extends Annotation>> types) {
    for (Class<? extends Annotation> type : types) {
      Annotation annotation = method.getAnnotation(type);
      if (annotation != null) {
        return type;
      }
    }
    return null;
  }
  
  private LanguageDriver getLanguageDriver(Method method) {
    Lang lang = method.getAnnotation(Lang.class);
    Class<?> langClass = null;
    if (lang != null) {
      langClass = lang.value();
    }
    return assistant.getLanguageDriver(langClass);
  }
  
  private SqlSource getSqlSourceFromAnnotations(Method method, Class<?> parameterType, LanguageDriver languageDriver) {
    try {
      Class<? extends Annotation> sqlAnnotationType = getSqlAnnotationType(method);
      Class<? extends Annotation> sqlProviderAnnotationType = getSqlProviderAnnotationType(method);
      if (sqlAnnotationType != null) {
        if (sqlProviderAnnotationType != null) {
          throw new BindingException("You cannot supply both a static SQL and SqlProvider to method named " + method.getName());
        }
        Annotation sqlAnnotation = method.getAnnotation(sqlAnnotationType);
        final String[] strings = (String[]) sqlAnnotation.getClass().getMethod("value").invoke(sqlAnnotation);
        return buildSqlSourceFromStrings(strings, parameterType, languageDriver);
      } else if (sqlProviderAnnotationType != null) {
        Annotation sqlProviderAnnotation = method.getAnnotation(sqlProviderAnnotationType);
        return new ProviderSqlSource(assistant.getConfiguration(), sqlProviderAnnotation);
      } else {
        return buildSqlSourceFromStrings(new String[]{buildSqlFromMethodName(method)}, parameterType, languageDriver);
      }
    } catch (Exception e) {
      throw new BuilderException("Could not find value method on SQL annotation.  Cause: " + e, e);
    }
  }
  
  private String buildSqlFromMethodName(Method method) {
    SqlCommandType commandType = getSqlCommandType(method);
    Map<String, Class<?>> typeMap = entityExpress.getColumnTypeMap();
    Map<String, String> columnMap = entityExpress.getColumnMap();
    Map<String, Class<?>> parameterMap = getParameters(method);
    StringBuilder builder = new StringBuilder();
    if (commandType == SqlCommandType.SELECT) {
      builder.append("<script>select ")
        .append(columnMap.values().stream().collect(Collectors.joining(",")))
        .append(" from ")
        .append(entityExpress.getName())
        .append("<where>");
      parameterMap.forEach((param, type) ->
        builder.append(" and ")
          .append(columnMap.get(param))
          .append("=#{")
          .append(param)
          .append(",javaType=")
          .append(type.getCanonicalName())
          .append("}"));
      builder.append("</where></script>");
    } else if (commandType == SqlCommandType.INSERT) {
      builder.append("<script>insert into ")
        .append(entityExpress.getName())
        .append(columnMap.values()
          .stream()
          .collect(Collectors.joining(",", "(", ")")))
        .append(" values ");
      Map.Entry<String, Class<?>> parameter = parameterMap.entrySet().iterator().next();
      if (parameter == null) {
        throw new RuntimeException("Insert needs parameter");
      }
      if (Collection.class.isAssignableFrom(parameter.getValue())) {
        builder.append("<foreach collection=\"collection\" item=\"item\" index=\"index\" separator=\",\">")
          .append(typeMap.entrySet()
            .stream()
            .map(e -> "#{item." + e.getKey() + ",javaType=" + e.getValue().getCanonicalName() + "}")
            .collect(Collectors.joining(",", "(", ")")))
          .append("</foreach>");
      } else {
        builder.append(typeMap.entrySet()
          .stream()
          .map(e -> "#{" + e.getKey() + ",javaType=" + e.getValue().getCanonicalName() + "}")
          .collect(Collectors.joining(",", "(", ")")));
      }
      builder.append("</script>");
    } else if (commandType == SqlCommandType.UPDATE) {
      Map.Entry<String, Class<?>> parameter = parameterMap.entrySet().iterator().next();
      if (parameter == null) {
        throw new RuntimeException("Insert needs parameter");
      }
      if (Collection.class.isAssignableFrom(parameter.getValue())) {
        builder.append("<script><foreach collection=\"collection\" item=\"item\" index=\"index\" open=\"\" close=\"\" separator=\";\">update ")
          .append(entityExpress.getName())
          .append("<set>")
          .append(columnMap.entrySet()
            .stream()
            .filter(e -> !e.getKey().equals(entityExpress.getPrimaryProperty()))
            .map(e -> e.getValue() + "=#{item." + e.getKey() + ",javaType=" + typeMap.get(e.getKey()).getCanonicalName()+ "}")
            .collect(Collectors.joining(","))
          ).append("</set> where ")
          .append(entityExpress.getPrimaryColumn())
          .append("=#{item.")
          .append(entityExpress.getPrimaryProperty())
          .append("}</foreach></script>");
      } else {
        builder.append("<script>update ")
          .append(entityExpress.getName())
          .append("<set>")
          .append(columnMap.entrySet()
            .stream()
            .filter(e -> !e.getKey().equals(entityExpress.getPrimaryProperty()))
            .map(e -> e.getValue() + "=#{" + e.getKey() + ",javaType=" + typeMap.get(e.getKey()).getCanonicalName()+ "}")
            .collect(Collectors.joining(","))
          ).append("</set> where ")
          .append(entityExpress.getPrimaryColumn())
          .append("=#{")
          .append(entityExpress.getPrimaryProperty())
          .append(",javaType=")
          .append(entityExpress.getPrimaryType().getCanonicalName())
          .append("}</script>");
      }
    }
    return builder.toString();
  }
  
  private Map<String, Class<?>> getParameters(Method method) {
    Map<String, Class<?>> parameterMap = new HashMap<>();
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; ++i) {
      MethodParameter parameter = new MethodParameter(method, i);
      Param param = parameter.getParameterAnnotation(Param.class);
      String key = parameters[i].getSimpleName();
      if (param != null) {
        key = param.value();
      }
      parameterMap.put(key, parameters[i]);
    }
    return parameterMap;
  }
  
  private Class<?> getReturnType(Method method) {
    Class<?> returnType = method.getReturnType();
    Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
    if (resolvedReturnType instanceof Class) {
      returnType = (Class<?>) resolvedReturnType;
      if (void.class.equals(returnType)) {
        ResultType rt = method.getAnnotation(ResultType.class);
        if (rt != null) {
          returnType = rt.value();
        }
      }
    } else if (resolvedReturnType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      if (Collection.class.isAssignableFrom(rawType)) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          Type returnTypeParameter = actualTypeArguments[0];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          } else if (returnTypeParameter instanceof ParameterizedType) {
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          } else if (returnTypeParameter instanceof GenericArrayType) {
            Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
            returnType = Array.newInstance(componentType, 0).getClass();
          }
        }
      } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 2) {
          Type returnTypeParameter = actualTypeArguments[1];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          } else if (returnTypeParameter instanceof ParameterizedType) {
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          }
        }
      }
    }
    
    return returnType;
  }
  
  private void loadXmlResource() {
    if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
      String xmlResource = type.getName().replace('.', '/') + ".xml";
      InputStream inputStream = null;
      try {
        inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
      } catch (IOException e) {
        // ignore, resource is not required
      }
      if (inputStream != null) {
        XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource, configuration.getSqlFragments(), type.getName());
        xmlParser.parse();
      }
    }
  }
  
  private void parseCache() {
    CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
    if (cacheDomain != null) {
      Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
      Long flushInterval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
      assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInterval, size, cacheDomain.readWrite(), cacheDomain.blocking(), null);
    }
  }
  
  private void parseCacheRef() {
    CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
    if (cacheDomainRef != null) {
      assistant.useCacheRef(cacheDomainRef.value().getName());
    }
  }
  
  private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
    final StringBuilder sql = new StringBuilder();
    for (String fragment : strings) {
      sql.append(fragment);
      sql.append(" ");
    }
    return languageDriver.createSqlSource(configuration, sql.toString().trim(), parameterTypeClass);
  }
  
  private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
    String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    Class<?> resultTypeClass = selectKeyAnnotation.resultType();
    StatementType statementType = selectKeyAnnotation.statementType();
    String keyProperty = selectKeyAnnotation.keyProperty();
    String keyColumn = selectKeyAnnotation.keyColumn();
    boolean executeBefore = selectKeyAnnotation.before();
    
    // defaults
    boolean useCache = false;
    KeyGenerator keyGenerator = new NoKeyGenerator();
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;
    
    SqlSource sqlSource = buildSqlSourceFromStrings(selectKeyAnnotation.statement(), parameterTypeClass, languageDriver);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;
    
    assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum,
      flushCache, useCache, false,
      keyGenerator, keyProperty, keyColumn, null, languageDriver, null);
    
    id = assistant.applyCurrentNamespace(id, false);
    
    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
    configuration.addKeyGenerator(id, answer);
    return answer;
  }
  
  private String nullOrEmpty(String value) {
    return value == null || value.trim().length() == 0 ? null : value;
  }
}
