package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.anno.Not;
import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class DeleteBuilder implements SqlBuilder {
    
    @Override
    public String buildSql(Class<?> mapper, Method method, EntityPortray entityPortray) {
        StringBuilder builder = new StringBuilder();
        Map<String, String> columnMap = entityPortray.getColumnMap();
        Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
        Map<String, MethodParameter> parameterMap = MapperUtils.getMethodParameters(method);
        builder.append("<script>delete from ")
            .append(entityPortray.getName())
            .append("<where>");
        parameterMap.forEach((param, type) -> {
            if (Collection.class.isAssignableFrom(type.getParameterType())) {
                builder.append(" and ")
                    .append(entityPortray.getClumn(param))
                    .append(type.getParameterAnnotation(Not.class) == null ? " " : " not ")
                    .append("in ")
                    .append("<foreach collection=\"")
                    .append(param)
                    .append("\" item=\"item\" index=\"index\" open=\"(\" separator=\",\" close=\")\" >")
                    .append("#{item}")
                    .append("</foreach>");
            } else {
                builder.append(" and ")
                    .append(columnMap.get(param))
                    .append(type.getParameterAnnotation(Not.class) == null ? "" : "!")
                    .append("=")
                    .append(MapperUtils.buildTypeValue(param, type.getParameterType(), "", typeHandlers.get(param)));
            }
        });
        builder.append("</where></script>");
        return builder.toString();
    }
}
