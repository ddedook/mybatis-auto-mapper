package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperConfig;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import javax.persistence.GeneratedValue;
import java.lang.reflect.Method;

public class KeyBuilder {
    
    private Configuration configuration;
    private MapperBuilderAssistant assistant;
    private KeyGenerator keyGenerator;
    private String keyProperty = "id";
    private String keyColumn = null;
    
    public KeyBuilder(Configuration configuration, MapperBuilderAssistant assistant) {
        this.configuration = configuration;
        this.assistant = assistant;
    }
    
    public void buildKey(SqlCommandType sqlCommandType, Method method, String mappedStatementId, EntityPortray entityPortray, Options options, LanguageDriver languageDriver) {
        if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
            // first check for SelectKey annotation - that overrides everything else
            keyProperty = entityPortray.getPrimaryProperty();
            SelectKey selectKey = method.getAnnotation(SelectKey.class);
            if (selectKey != null) {
                keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, MapperUtils.getParameterType(method), languageDriver);
                keyProperty = selectKey.keyProperty();
            } else if (options == null) {
                if (sqlCommandType == SqlCommandType.INSERT) {
                    keyGenerator = keyGeneratorFromEntity(entityPortray);
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
    }
    
    private KeyGenerator keyGeneratorFromEntity(EntityPortray entityPortray) {
        GeneratedValue generatedValue = entityPortray.getGeneratedValue();
        if (generatedValue != null) {
            String generator = generatedValue.generator();
            if (generator.equals("uuid") || generator.isEmpty()) {
                return MapperConfig.getGenerator(generator);
            }
        }
        return null;
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
        
        SqlSource sqlSource = MapperUtils.buildSqlSourceFromStrings(configuration, selectKeyAnnotation.statement(), parameterTypeClass, languageDriver);
        SqlCommandType sqlCommandType = SqlCommandType.SELECT;
        
        assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterTypeClass,
            resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, false,
            keyGenerator, keyProperty, keyColumn, null, languageDriver, null);
        id = assistant.applyCurrentNamespace(id, false);
        MappedStatement keyStatement = configuration.getMappedStatement(id, false);
        SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
        configuration.addKeyGenerator(id, answer);
        return answer;
    }
    
    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }
    
    public String getKeyProperty() {
        return keyProperty;
    }
    
    public String getKeyColumn() {
        return keyColumn;
    }
}
