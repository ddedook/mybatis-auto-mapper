package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperConfig;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateBuilder implements SqlBuilder {
    
    @Override
    public String buildSql(Class<?> mapper, Method method, EntityPortray entityPortray) {
        StringBuilder builder = new StringBuilder();
        Map<String, Class<?>> parameterMap = MapperUtils.getParameters(method);
        Map.Entry<String, Class<?>> parameter = parameterMap.entrySet().iterator().next();
        if (parameter == null) {
            throw new RuntimeException("Update needs parameter");
        }
        if (method.getName().startsWith("invalid")) {
            doInvalid(builder, parameter, entityPortray);
        } else {
            doUpdate(method, builder, parameter, entityPortray);
        }
        return builder.toString();
    }
    
    private void doInvalid(StringBuilder builder, Map.Entry<String, Class<?>> parameter, EntityPortray entityPortray) {
        builder.append("<script>update ")
            .append(entityPortray.getName())
            .append(" set ")
            .append(MapperConfig.getDelFlag())
            .append(" = 1 where ")
            .append(entityPortray.getPrimaryColumn());
        if (Collection.class.isAssignableFrom(parameter.getValue())) {
            builder.append(" in ")
                .append("<foreach collection=\"collection\" item=\"item\" index=\"index\" open=\"(\" separator=\",\" close=\")\" >")
                .append("#{item}")
                .append("</foreach>");
        } else {
            builder.append("=#{id}");
        }
        builder.append("</script>");
    }
    
    private void doUpdate(Method method, StringBuilder builder, Map.Entry<String, Class<?>> parameter, EntityPortray entityPortray) {
        Map<String, Class<?>> typeMap = entityPortray.getColumnTypeMap();
        Map<String, String> columnMap = entityPortray.getColumnMap();
        Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
        if (Collection.class.isAssignableFrom(parameter.getValue())) {
            boolean needCheck = "updateSelectiveAll".equals(method.getName());
            builder.append("<script><foreach collection=\"collection\" item=\"item\" index=\"index\" open=\"\" close=\"\" separator=\";\">update ")
                .append(entityPortray.getName())
                .append("<set>")
                .append(columnMap.entrySet()
                    .stream()
                    .filter(e -> !e.getKey().equals(entityPortray.getPrimaryProperty()))
                    .map(e -> buildUpdate(needCheck, "item.", e, typeMap.get(e.getKey()), typeHandlers.get(e.getKey())))
                    .collect(Collectors.joining())
                ).append("</set> where ")
                .append(entityPortray.getPrimaryColumn())
                .append("=#{item.")
                .append(entityPortray.getPrimaryProperty())
                .append("}</foreach></script>");
        } else {
            boolean needCheck = "updateSelective".equals(method.getName());
            builder.append("<script>update ")
                .append(entityPortray.getName())
                .append("<set>")
                .append(columnMap.entrySet()
                    .stream()
                    .filter(e -> !e.getKey().equals(entityPortray.getPrimaryProperty()))
                    .map(e -> buildUpdate(needCheck, "", e, typeMap.get(e.getKey()), typeHandlers.get(e.getKey())))
                    .collect(Collectors.joining())
                ).append("</set> where ")
                .append(entityPortray.getPrimaryColumn())
                .append("=")
                .append(MapperUtils.buildTypeValue(entityPortray.getPrimaryProperty(), entityPortray.getPrimaryType(),
                    "", typeHandlers.get(entityPortray.getPrimaryProperty())))
                .append("</script>");
        }
    }
    
    private String buildUpdate(boolean needCheck, String prefix, Map.Entry<String, String> entry, Class<?> type, Class<? extends TypeHandler> typeHandler) {
        StringBuilder builder = new StringBuilder();
        if (needCheck) {
            builder.append("<if test=\"").append(prefix).append(entry.getKey()).append(" != null\">");
        }
        builder.append(entry.getValue())
            .append("=")
            .append(MapperUtils.buildTypeValue(prefix + entry.getKey(), type, "", typeHandler))
            .append(",");
        if (needCheck) {
            builder.append("</if>");
        }
        return builder.toString();
    }
}
