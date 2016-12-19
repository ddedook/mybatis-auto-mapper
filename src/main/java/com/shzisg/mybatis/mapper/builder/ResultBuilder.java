package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;

import javax.persistence.Table;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResultBuilder {
    
    public String buildResultMap(Configuration configuration, Class<?> type, Method method, boolean isSelect) {
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
        }
        return resultMapId;
    }
    
    protected TypeHandler<?> resolveTypeHandler(Configuration configuration, Class<?> javaType, Class<? extends TypeHandler> typeHandlerType) {
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
