package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class InsertBuilder implements SqlBuilder {
    
    @Override
    public String buildSql(Class<?> mapper, Method method, EntityPortray entityPortray) {
        StringBuilder builder = new StringBuilder();
        Map<String, Class<?>> typeMap = entityPortray.getColumnTypeMap();
        Map<String, String> columnMap = entityPortray.getColumnMap();
        Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
        Map<String, Class<?>> parameterMap = MapperUtils.getParameters(method);
        builder.append("<script>insert into ")
            .append(entityPortray.getName())
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
                    .map(e -> MapperUtils.buildTypeValue(e.getKey(), e.getValue(), "item.", typeHandlers.get(e.getKey())))
                    .collect(Collectors.joining(",", "(", ")")))
                .append("</foreach>");
        } else {
            builder.append(typeMap.entrySet()
                .stream()
                .map(e -> MapperUtils.buildTypeValue(e.getKey(), e.getValue(), "", typeHandlers.get(e.getKey())))
                .collect(Collectors.joining(",", "(", ")")));
        }
        builder.append("</script>");
        return builder.toString();
    }
}
