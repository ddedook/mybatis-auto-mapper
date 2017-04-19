package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

import javax.persistence.Table;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultBuilder {
    
    private Configuration configuration;
    private MapperBuilderAssistant assistant;
    private Class<?> type;
    
    public String buildResultMap(Configuration configuration, MapperBuilderAssistant assistant, Class<?> type, Method method, boolean isSelect) {
        this.configuration = configuration;
        this.assistant = assistant;
        this.type = type;
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
            resultMapId = parseResultMap(configuration, type, method);
        }
        return resultMapId;
    }
    
    private String parseResultMap(Configuration configuration, Class<?> type, Method method) {
        String resultMapId = type.getName() + "." + method.getName() + "-Inline";
        Class<?> returnType = MapperUtils.getReturnType(method, type);
        if (returnType.getAnnotation(Table.class) != null) {
            EntityPortray entityPortray = new EntityPortray(returnType);
            Map<String, String> columnMap = entityPortray.getColumnMap();
            Map<String, Class<?>> typeMap = entityPortray.getColumnTypeMap();
            Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
            List<ResultMapping> resultMappings = new ArrayList<>();
            columnMap.forEach((prop, column) -> {
                ResultMapping.Builder builder = new ResultMapping.Builder(
                    configuration,
                    prop,
                    column,
                    typeMap.get(prop)
                );
                if (typeHandlers.containsKey(prop)) {
                    builder.typeHandler(resolveTypeHandler(configuration, typeMap.get(prop), typeHandlers.get(prop)));
                }
                resultMappings.add(builder.build());
            });
            org.apache.ibatis.mapping.ResultMap.Builder builder =
                new org.apache.ibatis.mapping.ResultMap.Builder(configuration, resultMapId, returnType, resultMappings);
            configuration.addResultMap(builder.build());
        } else {
            resultMapId = parseResultMap(method, returnType, type);
        }
        return resultMapId;
    }
    
    private String parseResultMap(Method method, Class<?> returnType, Class<?> type) {
        ConstructorArgs args = method.getAnnotation(ConstructorArgs.class);
        Results results = method.getAnnotation(Results.class);
        TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
        String resultMapId = generateResultMapName(method, type);
        applyResultMap(resultMapId, returnType, argsIf(args), resultsIf(results), typeDiscriminator);
        return resultMapId;
    }
    
    private String generateResultMapName(Method method, Class<?> type) {
        Results results = method.getAnnotation(Results.class);
        if (results != null && !results.id().isEmpty()) {
            return type.getName() + "." + results.id();
        }
        StringBuilder suffix = new StringBuilder();
        for (Class<?> c : method.getParameterTypes()) {
            suffix.append("-");
            suffix.append(c.getSimpleName());
        }
        if (suffix.length() < 1) {
            suffix.append("-void");
        }
        return type.getName() + "." + method.getName() + suffix;
    }
    
    private Result[] resultsIf(Results results) {
        return results == null ? new Result[0] : results.value();
    }
    
    private Arg[] argsIf(ConstructorArgs args) {
        return args == null ? new Arg[0] : args.value();
    }
    
    private boolean hasNestedSelect(Result result) {
        if (result.one().select().length() > 0 && result.many().select().length() > 0) {
            throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
        }
        return result.one().select().length() > 0 || result.many().select().length() > 0;
    }
    
    private boolean isLazy(Result result) {
        boolean isLazy = configuration.isLazyLoadingEnabled();
        if (result.one().select().length() > 0 && FetchType.DEFAULT != result.one().fetchType()) {
            isLazy = (result.one().fetchType() == FetchType.LAZY);
        } else if (result.many().select().length() > 0 && FetchType.DEFAULT != result.many().fetchType()) {
            isLazy = (result.many().fetchType() == FetchType.LAZY);
        }
        return isLazy;
    }
    
    private String nestedSelectId(Result result) {
        String nestedSelect = result.one().select();
        if (nestedSelect.length() < 1) {
            nestedSelect = result.many().select();
        }
        if (!nestedSelect.contains(".")) {
            nestedSelect = type.getName() + "." + nestedSelect;
        }
        return nestedSelect;
    }
    
    private String nullOrEmpty(String value) {
        return value == null || value.trim().length() == 0 ? null : value;
    }
    
    private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results, TypeDiscriminator discriminator) {
        List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
        applyConstructorArgs(args, returnType, resultMappings);
        applyResults(results, returnType, resultMappings);
        Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
        // TODO add AutoMappingBehaviour
        assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
        createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
    }
    
    private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
        if (discriminator != null) {
            String column = discriminator.column();
            Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
            JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
            @SuppressWarnings("unchecked")
            Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
                (discriminator.typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
            Case[] cases = discriminator.cases();
            Map<String, String> discriminatorMap = new HashMap<String, String>();
            for (Case c : cases) {
                String value = c.value();
                String caseResultMapId = resultMapId + "-" + value;
                discriminatorMap.put(value, caseResultMapId);
            }
            return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
        }
        return null;
    }
    
    private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
        if (discriminator != null) {
            for (Case c : discriminator.cases()) {
                String caseResultMapId = resultMapId + "-" + c.value();
                List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
                // issue #136
                applyConstructorArgs(c.constructArgs(), resultType, resultMappings);
                applyResults(c.results(), resultType, resultMappings);
                // TODO add AutoMappingBehaviour
                assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
            }
        }
    }
    
    private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
        for (Result result : results) {
            List<ResultFlag> flags = new ArrayList<ResultFlag>();
            if (result.id()) {
                flags.add(ResultFlag.ID);
            }
            @SuppressWarnings("unchecked")
            Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
                ((result.typeHandler() == UnknownTypeHandler.class) ? null : result.typeHandler());
            ResultMapping resultMapping = assistant.buildResultMapping(
                resultType,
                nullOrEmpty(result.property()),
                nullOrEmpty(result.column()),
                result.javaType() == void.class ? null : result.javaType(),
                result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
                hasNestedSelect(result) ? nestedSelectId(result) : null,
                null,
                null,
                null,
                typeHandler,
                flags,
                null,
                null,
                isLazy(result));
            resultMappings.add(resultMapping);
        }
    }
    
    private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings) {
        for (Arg arg : args) {
            List<ResultFlag> flags = new ArrayList<ResultFlag>();
            flags.add(ResultFlag.CONSTRUCTOR);
            if (arg.id()) {
                flags.add(ResultFlag.ID);
            }
            @SuppressWarnings("unchecked")
            Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
                (arg.typeHandler() == UnknownTypeHandler.class ? null : arg.typeHandler());
            ResultMapping resultMapping = assistant.buildResultMapping(
                resultType,
                null,
                nullOrEmpty(arg.column()),
                arg.javaType() == void.class ? null : arg.javaType(),
                arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(),
                nullOrEmpty(arg.select()),
                nullOrEmpty(arg.resultMap()),
                null,
                null,
                typeHandler,
                flags,
                null,
                null,
                false);
            resultMappings.add(resultMapping);
        }
    }
    
    private TypeHandler<?> resolveTypeHandler(Configuration configuration, Class<?> javaType, Class<? extends TypeHandler> typeHandlerType) {
        if (typeHandlerType == null) {
            return null;
        }
        // javaType ignored for injected handlers see issue #746 for full detail
        TypeHandler<?> handler = configuration.getTypeHandlerRegistry().getMappingTypeHandler((Class<? extends TypeHandler<?>>) typeHandlerType);
        if (handler == null) {
            // not in registry, create a new one
            handler = configuration.getTypeHandlerRegistry().getInstance(javaType, typeHandlerType);
        }
        return handler;
    }
    
}
