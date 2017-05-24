package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.anno.Not;
import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperConfig;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import com.shzisg.mybatis.mapper.auto.OrderBy;
import com.shzisg.mybatis.mapper.page.PageRequest;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.core.MethodParameter;

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
//        Map<String, Class<?>> parameterMap = MapperUtils.getParameters(method);
        Map<String, MethodParameter> parameterMap = MapperUtils.getMethodParameters(method);
        if (columnMap.containsKey(MapperConfig.getDelFlag()) && method.getName().endsWith("Valid") && !parameterMap.containsKey(MapperConfig.getDelFlag())) {
            parameterMap.put("__del_flag__", null);
        }
        builder.append("<script>select ")
            .append(returnPortray.getColumnMap().values().stream().collect(Collectors.joining(",")))
            .append(" from ")
            .append(entityPortray.getName())
            .append("<where>");
        parameterMap.forEach((param, paraType) -> {
            if (!param.equals("__del_flag__") && PageRequest.class.isAssignableFrom(paraType.getParameterType())) {
                return;
            }
            if (param.equals("__del_flag__")) {
                builder.append(" and ")
                    .append(columnMap.get(MapperConfig.getDelFlag()))
                    .append("=0");
            } else if (Collection.class.isAssignableFrom(paraType.getParameterType())) {
                builder.append(" and ")
                    .append(columnMap.get(param))
                    .append(paraType.getParameterAnnotation(Not.class) == null ? "" : " not")
                    .append(" in ")
                    .append("<foreach item=\"item\" index=\"index\" collection=\"")
                    .append(param)
                    .append("\" open=\"(\" separator=\",\" close=\")\">")
                    .append("#{item}</foreach>");
            } else {
                builder.append(" and ")
                    .append(columnMap.get(param))
                    .append(paraType.getParameterAnnotation(Not.class) == null ? "" : "!")
                    .append("=")
                    .append(MapperUtils.buildTypeValue(param, paraType.getParameterType(), "", typeHandlers.get(param)));
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
