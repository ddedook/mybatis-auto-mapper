package com.shzisg.mybatis.mapper.auto;

import com.shzisg.mybatis.mapper.page.Page;
import com.shzisg.mybatis.mapper.page.PageRequest;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.core.MethodParameter;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapperUtils {
    
    public static Map<String, Class<?>> getParameters(Method method) {
        Map<String, Class<?>> parameterMap = new HashMap<>();
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; ++i) {
            MethodParameter parameter = new MethodParameter(method, i);
            Param param = parameter.getParameterAnnotation(Param.class);
            String key = parameters[i].getSimpleName();
            if (param != null) {
                key = param.value();
            }
            parameterMap.put(key, parameters[i]);
        }
        return parameterMap;
    }
    
    public static Class<?> getReturnType(Method method, Class<?> type) {
        Class<?> returnType = method.getReturnType();
        Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
        if (resolvedReturnType instanceof Class) {
            returnType = (Class<?>) resolvedReturnType;
            if (void.class.equals(returnType)) {
                ResultType rt = method.getAnnotation(ResultType.class);
                if (rt != null) {
                    returnType = rt.value();
                }
            }
        } else if (resolvedReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (Collection.class.isAssignableFrom(rawType) || Page.class.isAssignableFrom(rawType)) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 1) {
                    Type returnTypeParameter = actualTypeArguments[0];
                    if (returnTypeParameter instanceof Class<?>) {
                        returnType = (Class<?>) returnTypeParameter;
                    } else if (returnTypeParameter instanceof ParameterizedType) {
                        returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
                    } else if (returnTypeParameter instanceof GenericArrayType) {
                        Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
                        returnType = Array.newInstance(componentType, 0).getClass();
                    }
                }
            } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 2) {
                    Type returnTypeParameter = actualTypeArguments[1];
                    if (returnTypeParameter instanceof Class<?>) {
                        returnType = (Class<?>) returnTypeParameter;
                    } else if (returnTypeParameter instanceof ParameterizedType) {
                        returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
                    }
                }
            }
        }
        
        return returnType;
    }
    
    public static SqlSource buildSqlSourceFromStrings(Configuration configuration, String[] strings, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
        final StringBuilder sql = new StringBuilder();
        for (String fragment : strings) {
            sql.append(fragment);
            sql.append(" ");
        }
        return languageDriver.createSqlSource(configuration, sql.toString().trim(), parameterTypeClass);
    }
    
    public static Class<?> getParameterType(Method method) {
        Class<?> parameterType = null;
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> type : parameterTypes) {
            if (!PageRequest.class.isAssignableFrom(type) && !ResultHandler.class.isAssignableFrom(type)) {
                if (parameterType == null) {
                    parameterType = type;
                } else {
                    parameterType = MapperMethod.ParamMap.class;
                }
            }
        }
        return parameterType;
    }
    
    public static String buildTypeValue(String property, Class<?> type, String prefix, Class<?> handler) {
        StringBuilder builder = new StringBuilder();
        builder.append("#{")
            .append(prefix)
            .append(property)
            .append(",javaType=")
            .append(type.getCanonicalName());
        if (handler != null) {
            builder.append(",typeHandler=")
                .append(handler.getCanonicalName());
        }
        builder.append("}");
        return builder.toString();
    }
}
