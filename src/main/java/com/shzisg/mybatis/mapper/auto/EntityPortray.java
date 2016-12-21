package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.TypeHandler;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EntityPortray {
  private String name;
  private Map<String, String> columnMap = new HashMap<>();
  private Map<String, Class<?>> columnTypeMap = new HashMap<>();
  private Map<String, Class<? extends TypeHandler>> columnTypeHandlers = new HashMap<>();
  private Class<?> entityClass;
  private String primaryProperty;
  private String primaryColumn;
  private Class<?> primaryType;
  private GeneratedValue generatedValue;
  
  public EntityPortray(Class<?> entity) {
    if (entity.getAnnotation(Table.class) != null) {
      this.entityClass = entity;
    } else {
      this.entityClass = getEntityType(entity);
    }
    parse();
  }
  
  private void parse() {
    Table table = entityClass.getAnnotation(Table.class);
    if (table != null) {
      name = table.name();
    } else {
      name = entityClass.getSimpleName().toLowerCase();
    }
    for (Field field : entityClass.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())
        || Modifier.isFinal(field.getModifiers())
        || field.getAnnotation(Transient.class) != null) {
        continue;
      }
      Column column = field.getDeclaredAnnotation(Column.class);
      if (field.getAnnotation(Id.class) != null) {
        primaryColumn = column.name();
        primaryProperty = field.getName();
        primaryType = field.getType();
        generatedValue = field.getAnnotation(GeneratedValue.class);
      }
      String columnName = field.getName();
      if (column != null) {
        columnName = column.name();
      }
      columnMap.put(field.getName(), columnName);
      columnTypeMap.put(field.getName(), field.getType());
      Enumerated enumerated = field.getAnnotation(Enumerated.class);
      if (enumerated != null) {
        if (enumerated.value() == EnumType.STRING) {
          columnTypeHandlers.put(field.getName(), EnumTypeHandler.class);
        } else {
          columnTypeHandlers.put(field.getName(), EnumOrdinalTypeHandler.class);
        }
      }
    }
  }
  
  private Class<?> getEntityType(Class<?> mapper) {
    Type[] types = mapper.getGenericInterfaces();
    for (Type type : types) {
      if (type instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class rawType = (Class) parameterizedType.getRawType();
        if (AutoMapper.class.isAssignableFrom(rawType)) {
          Class entityType = (Class) parameterizedType.getActualTypeArguments()[0];
          if (entityType.isAnnotationPresent(Table.class)) {
            return entityType;
          }
        }
      }
    }
    return null;
  }
  
  public String getPrimaryProperty() {
    return primaryProperty;
  }
  
  public void setPrimaryProperty(String primaryProperty) {
    this.primaryProperty = primaryProperty;
  }
  
  public String getPrimaryColumn() {
    return primaryColumn;
  }
  
  public void setPrimaryColumn(String primaryColumn) {
    this.primaryColumn = primaryColumn;
  }
  
  public Class<?> getPrimaryType() {
    return primaryType;
  }
  
  public void setPrimaryType(Class<?> primaryType) {
    this.primaryType = primaryType;
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
  
  public Map<String, Class<? extends TypeHandler>> getColumnTypeHandlers() {
    return columnTypeHandlers;
  }
  
  public void setColumnTypeHandlers(Map<String, Class<? extends TypeHandler>> columnTypeHandlers) {
    this.columnTypeHandlers = columnTypeHandlers;
  }
}
