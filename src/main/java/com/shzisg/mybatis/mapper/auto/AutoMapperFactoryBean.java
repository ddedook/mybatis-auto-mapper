package com.shzisg.mybatis.mapper.auto;

import org.mybatis.spring.mapper.MapperFactoryBean;

public class AutoMapperFactoryBean<T> extends MapperFactoryBean<T> {
    
    public AutoMapperFactoryBean() {
        //intentionally empty
    }

    public AutoMapperFactoryBean(Class<T> mapperInterface) {
        super(mapperInterface);
    }

    @Override
    protected void checkDaoConfig() {
        Class mapperInterface = getMapperInterface();
        new MapperAnnotationAutoBuilder(getSqlSession().getConfiguration(), mapperInterface)
          .parse();
        super.checkDaoConfig();
    }
}
