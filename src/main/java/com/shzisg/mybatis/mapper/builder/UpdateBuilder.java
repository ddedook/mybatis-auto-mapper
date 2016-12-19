package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateBuilder implements SqlBuilder {
    
    @Override
    public String buildSql(Method method, EntityPortray entityPortray) {
        StringBuilder builder = new StringBuilder();
        Map<String, Class<?>> typeMap = entityPortray.getColumnTypeMap();
        Map<String, String> columnMap = entityPortray.getColumnMap();
        Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
        Map<String, Class<?>> parameterMap = MapperUtils.getParameters(method);
        Map.Entry<String, Class<?>> parameter = parameterMap.entrySet().iterator().next();
        if (parameter == null) {
            throw new RuntimeException("Insert needs parameter");
        }
        if (Collection.class.isAssignableFrom(parameter.getValue())) {
            builder.append("<script><foreach collection=\"collection\" item=\"item\" index=\"index\" open=\"\" close=\"\" separator=\";\">update ")
                .append(entityPortray.getName())
                .append("<set>")
                .append(columnMap.entrySet()
                    .stream()
                    .filter(e -> !e.getKey().equals(entityPortray.getPrimaryProperty()))
                    .map(e -> e.getValue() + "=" + MapperUtils.buildTypeValue(e.getKey(), typeMap.get(e.getKey()), "item.", typeHandlers.get(e.getKey())))
                    .collect(Collectors.joining(","))
                ).append("</set> where ")
                .append(entityPortray.getPrimaryColumn())
                .append("=#{item.")
                .append(entityPortray.getPrimaryProperty())
                .append("}</foreach></script>");
        } else {
            builder.append("<script>update ")
                .append(entityPortray.getName())
                .append("<set>")
                .append(columnMap.entrySet()
                    .stream()
                    .filter(e -> !e.getKey().equals(entityPortray.getPrimaryProperty()))
                    .map(e -> e.getValue() + "=" + MapperUtils.buildTypeValue(e.getKey(), typeMap.get(e.getKey()), "", typeHandlers.get(e.getKey())))
                    .collect(Collectors.joining(","))
                ).append("</set> where ")
                .append(entityPortray.getPrimaryColumn())
                .append("=")
                .append(MapperUtils.buildTypeValue(entityPortray.getPrimaryProperty(), entityPortray.getPrimaryType(),
                    "", typeHandlers.get(entityPortray.getPrimaryProperty())))
                .append("</script>");
        }
        return builder.toString();
    }
}
