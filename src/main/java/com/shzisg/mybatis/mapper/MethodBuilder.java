package com.shzisg.mybatis.mapper;

import com.sun.tools.javac.util.Name;
import org.apache.ibatis.annotations.Select;

import javax.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class MethodBuilder {

    public List<MethodStructure> build(Class<?> mapper) {
        Class<?> entityClass = getEntityType(mapper);
        Method[] methods = mapper.getMethods();
        for (Method method : methods) {
            MethodStructure structure = new MethodStructure();
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Select) {

                }
            }
        }
        return null;
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
}
