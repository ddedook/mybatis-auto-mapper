package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;
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
  public String buildSql(Method method, EntityPortray entityPortray) {
    StringBuilder builder = new StringBuilder();
    Map<String, String> columnMap = entityPortray.getColumnMap();
    Map<String, Class<? extends TypeHandler>> typeHandlers = entityPortray.getColumnTypeHandlers();
    Map<String, Class<?>> parameterMap = MapperUtils.getParameters(method);
    builder.append("<script>select ")
      .append(columnMap.values().stream().collect(Collectors.joining(",")))
      .append(" from ")
      .append(entityPortray.getName())
      .append("<where>");
    parameterMap.forEach((param, type) -> {
      if (PageRequest.class.isAssignableFrom(type)) {
        return;
      }
      if (Collection.class.isAssignableFrom(type)) {
        builder.append(" and ")
          .append(columnMap.get(param))
          .append(" in ")
          .append("<foreach item=\"item\" index=\"index\" collection=\"")
          .append(param)
          .append("\" open=\"(\" separator=\",\" close=\")\">")
          .append("#{item}</foreach>");
      } else {
        builder.append(" and ")
          .append(columnMap.get(param))
          .append("=")
          .append(MapperUtils.buildTypeValue(param, type, "", typeHandlers.get(param)));
      }
    });
    builder.append("</where>");
    OrderBy orderBy = method.getAnnotation(OrderBy.class);
    if (orderBy != null) {
      builder.append(" order by ")
        .append(columnMap.getOrDefault(orderBy.value(), orderBy.value()))
        .append(orderBy.desc() ? "desc" : "asc");
      
    }
    builder.append("</script>");
    return builder.toString();
  }
}
