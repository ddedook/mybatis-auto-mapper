package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.binding.MapperProxy;
import org.apache.ibatis.binding.MapperProxyFactory;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoMapperProxyFactory<T> extends MapperProxyFactory<T> {
    
    private final Map<Method, AutoMapperMethod> methodCache = new ConcurrentHashMap<>();
    
    public AutoMapperProxyFactory(Class<T> mapperInterface) {
        super(mapperInterface);
    }
    
    @Override
    protected T newInstance(MapperProxy<T> mapperProxy) {
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private T newInstance(AutoMapperProxy<T> mapperProxy) {
        return (T) Proxy.newProxyInstance(getMapperInterface().getClassLoader(), new Class[]{getMapperInterface()}, mapperProxy);
    }
    
    @Override
    public T newInstance(SqlSession sqlSession) {
        final AutoMapperProxy<T> mapperProxy = new AutoMapperProxy<T>(sqlSession, this.getMapperInterface(),
            this.methodCache);
        return newInstance(mapperProxy);
    }
}
