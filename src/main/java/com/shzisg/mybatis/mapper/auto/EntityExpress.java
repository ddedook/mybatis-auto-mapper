package com.shzisg.mybatis.mapper.auto;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EntityExpress {
  private String name;
  private Map<String, String> columnMap;
  private Map<String, Class<?>> columnTypeMap;
  private Class<?> entityClass;
  private GeneratedValue generatedValue;
  
  public EntityExpress(Class<?> entity) {
    if (entity.getAnnotation(Table.class) != null) {
      this.entityClass = entity;
    } else {
      this.entityClass = getEntityType(entity);
    }
    parse();
  }
  
  private void parse() {
    this.columnMap = new HashMap<>();
    this.columnTypeMap = new HashMap<>();
    Table table = entityClass.getAnnotation(Table.class);
    if (table != null) {
      name = table.name();
    } else {
      name = entityClass.getSimpleName().toLowerCase();
    }
    for (Field field : entityClass.getDeclaredFields()) {
      Column column = field.getDeclaredAnnotation(Column.class);
      if (field.getAnnotation(Id.class) != null) {
        generatedValue = field.getAnnotation(GeneratedValue.class);
      }
      String columnName = field.getName();
      if (column != null) {
        columnName = column.name();
      }
      columnMap.put(field.getName(), columnName);
      columnTypeMap.put(field.getName(), field.getType());
    }
  }
  
  private Class<?> getEntityType(Class<?> mapper) {
    Type[] types = mapper.getGenericInterfaces();
    for (Type type : types) {
      if (type instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class rawType = (Class) parameterizedType.getRawType();
        if (Mapper.class.isAssignableFrom(rawType)) {
          Class entityType = (Class) parameterizedType.getActualTypeArguments()[0];
          if (entityType.isAnnotationPresent(Table.class)) {
            return entityType;
          }
        }
      }
    }
    return null;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Map<String, String> getColumnMap() {
    return columnMap;
  }
  
  public String getClumn(String property) {
    return columnMap.get(property);
  }
  
  public void setColumnMap(Map<String, String> columnMap) {
    this.columnMap = columnMap;
  }
  
  public Map<String, Class<?>> getColumnTypeMap() {
    return columnTypeMap;
  }
  
  public Class<?> columnType(String property) {
    return columnTypeMap.get(property);
  }
  
  public void setColumnTypeMap(Map<String, Class<?>> columnTypeMap) {
    this.columnTypeMap = columnTypeMap;
  }
  
  public Class<?> getEntityClass() {
    return entityClass;
  }
  
  public void setEntityClass(Class<?> entityClass) {
    this.entityClass = entityClass;
  }
  
  public GeneratedValue getGeneratedValue() {
    return generatedValue;
  }
  
  public void setGeneratedValue(GeneratedValue generatedValue) {
    this.generatedValue = generatedValue;
  }
}
