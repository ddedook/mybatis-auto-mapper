package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.sql.Statement;
import java.util.*;

public class UuidGenerator extends NoKeyGenerator {
  
  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    Configuration configuration = ms.getConfiguration();
    Collection<Object> parameters = toCollection(parameter);
    for (Object param : parameters) {
      MetaObject metaParam = configuration.newMetaObject(param);
      String[] key = ms.getKeyProperties();
      if (metaParam.hasSetter(key[0]) && metaParam.getValue(key[0]) != null) {
        metaParam.setValue(key[0], uuid());
      }
    }
  }
  
  private String uuid() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }
  
  @SuppressWarnings("unchecked")
  private Collection<Object> toCollection(Object parameter) {
    Object parameters = null;
    if (parameter instanceof Collection) {
      parameters = parameter;
    } else if (parameter instanceof Map) {
      Map parameterMap = (Map) parameter;
      if (parameterMap.containsKey("collection")) {
        parameters = parameterMap.get("collection");
      } else if (parameterMap.containsKey("list")) {
        parameters = parameterMap.get("list");
      } else if (parameterMap.containsKey("array")) {
        parameters = Arrays.asList((Object[]) (parameterMap.get("array")));
      }
    }
    
    if (parameters == null) {
      parameters = new ArrayList();
      ((Collection) parameters).add(parameter);
    }
    return (Collection) parameters;
  }
}
