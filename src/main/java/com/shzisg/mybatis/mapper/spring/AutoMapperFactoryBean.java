package com.shzisg.mybatis.mapper.spring;

import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.mybatis.spring.mapper.MapperFactoryBean;

public class AutoMapperFactoryBean<T> extends MapperFactoryBean<T> {

    public AutoMapperFactoryBean(){
        //intentionally empty
    }

    public AutoMapperFactoryBean(Class<T> mapperInterface) {
        super(mapperInterface);
    }

    @Override
    protected void checkDaoConfig() {
        super.checkDaoConfig();
        Class<T> mapperInterface = getMapperInterface();

    }
}
