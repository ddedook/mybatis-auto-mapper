package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public class AutoMapperProxy<T> implements InvocationHandler, Serializable {
    
    private static final long serialVersionUID = -6424540398559729838L;
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;
    private final Map<Method, AutoMapperMethod> methodCache;
    
    public AutoMapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, AutoMapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            try {
                return method.invoke(this, args);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        }
        final AutoMapperMethod mapperMethod = cachedMapperMethod(method);
        return mapperMethod.execute(sqlSession, args);
    }
    
    private AutoMapperMethod cachedMapperMethod(Method method) {
        return methodCache.computeIfAbsent(method, k ->
            new AutoMapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
    }
}
