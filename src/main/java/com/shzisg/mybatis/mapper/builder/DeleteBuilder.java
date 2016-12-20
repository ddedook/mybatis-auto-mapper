package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
import com.shzisg.mybatis.mapper.auto.MapperUtils;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class DeleteBuilder implements SqlBuilder {
    
    @Override
    public String buildSql(Method method, EntityPortray entityPortray) {
        StringBuilder builder = new StringBuilder();
        Map<String, String> columnMap = entityPortray.getColumnMap();
        Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
        Map<String, Class<?>> parameterMap = MapperUtils.getParameters(method);
        builder.append("<script>delete from ")
            .append(entityPortray.getName())
            .append("<where>");
        Class<?> onlyParameter = null;
        if (parameterMap.size() == 1) {
            Map.Entry<String, Class<?>> entry = parameterMap.entrySet().iterator().next();
            onlyParameter = entry.getValue();
        }
        if (onlyParameter != null && Collection.class.isAssignableFrom(onlyParameter)) {
            builder.append(" id in ")
                .append("<foreach collection=\"collection\" item=\"item\" index=\"index\" open=\"(\" separator=\",\" close=\")\" >")
                .append("#{item}")
                .append("</foreach>");
        } else {
            parameterMap.forEach((param, type) ->
                builder.append(" and ")
                    .append(columnMap.get(param))
                    .append("=")
                    .append(MapperUtils.buildTypeValue(param, type, "", typeHandlers.get(param))));
        }
        builder.append("</where></script>");
        return builder.toString();
    }
}
