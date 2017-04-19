package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperConfig;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import com.shzisg.mybatis.mapper.auto.OrderBy;
import com.shzisg.mybatis.mapper.page.PageRequest;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SelectBuilder implements SqlBuilder {
    
    @Override
    public String buildSql(Class<?> mapper, Method method, EntityPortray entityPortray) {
        StringBuilder builder = new StringBuilder();
        Class<?> returnType = MapperUtils.getReturnType(method, mapper);
        EntityPortray returnPortray = MapperUtils.getEntityPortray(mapper, returnType);
        Map<String, String> columnMap = entityPortray.getColumnMap();
        Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
        Map<String, Class<?>> parameterMap = MapperUtils.getParameters(method);
        if (columnMap.values().contains(MapperConfig.getDelFlag()) && method.getName().endsWith("Valid") && !parameterMap.containsKey(MapperConfig.getDelFlag())) {
            parameterMap.put("__del_flag__", String.class);
        }
        builder.append("<script>select ")
            .append(returnPortray.getColumnMap().values().stream().collect(Collectors.joining(",")))
            .append(" FROM ")
            .append(entityPortray.getName())
            .append("<where>");
        parameterMap.forEach((param, paraType) -> {
            if (PageRequest.class.isAssignableFrom(paraType)) {
                return;
            }
            if (Collection.class.isAssignableFrom(paraType)) {
                builder.append(" and ")
                    .append(columnMap.get(param))
                    .append(" in ")
                    .append("<foreach item=\"item\" index=\"index\" collection=\"")
                    .append(param)
                    .append("\" open=\"(\" separator=\",\" close=\")\">")
                    .append("#{item}</foreach>");
            } else if (param.equals("__del_flag__")) {
                builder.append(" and ")
                    .append(MapperConfig.getDelFlag())
                    .append("=0");
            } else {
                builder.append(" and ")
                    .append(columnMap.get(param))
                    .append("=")
                    .append(MapperUtils.buildTypeValue(param, paraType, "", typeHandlers.get(param)));
            }
        });
        builder.append("</where>");
        OrderBy orderBy = method.getAnnotation(OrderBy.class);
        if (orderBy != null) {
            if (!orderBy.orderSql().isEmpty()) {
                builder.append(orderBy.orderSql());
            } else if (!orderBy.value().isEmpty()) {
                builder.append(" order by ")
                    .append(columnMap.getOrDefault(orderBy.value(), orderBy.value()))
                    .append(orderBy.desc() ? " desc " : " asc ");
            }
        }
        builder.append("</script>");
        return builder.toString();
    }
}
