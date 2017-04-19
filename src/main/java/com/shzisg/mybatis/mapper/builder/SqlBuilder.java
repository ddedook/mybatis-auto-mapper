package com.shzisg.mybatis.mapper.builder;

import com.shzisg.mybatis.mapper.auto.EntityPortray;

import java.lang.reflect.Method;

public interface SqlBuilder {
    
    String buildSql(Class<?> mapper, Method method, EntityPortray entityPortray);
}
