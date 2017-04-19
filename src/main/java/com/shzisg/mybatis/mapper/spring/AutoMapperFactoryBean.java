package com.shzisg.mybatis.mapper.spring;

import com.shzisg.mybatis.mapper.auto.AutoMapper;
import com.shzisg.mybatis.mapper.auto.AutoMapperAnnotationBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.mapper.MapperFactoryBean;

import static org.springframework.util.Assert.notNull;

public class AutoMapperFactoryBean<T> extends MapperFactoryBean<T> {

    public AutoMapperFactoryBean() {
        //intentionally empty
    }

    public AutoMapperFactoryBean(Class<T> mapperInterface) {
        super(mapperInterface);
    }

    @Override
    protected void checkDaoConfig() {
        notNull(getSqlSession(), "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required");
        // config mapper
        Configuration configuration = getSqlSession().getConfiguration();
        if (!configuration.hasMapper(this.getMapperInterface())) {
            try {
                if (AutoMapper.class.isAssignableFrom(this.getMapperInterface())) {
                    new AutoMapperAnnotationBuilder(getSqlSession().getConfiguration(), this.getMapperInterface())
                        .parse();
                }
                configuration.addMapper(this.getMapperInterface());
            } catch (Exception e) {
                throw new IllegalArgumentException("Error while adding the mapper '" + this.getMapperInterface() + "' to configuration.", e);
            } finally {
                ErrorContext.instance().reset();
            }
        }
    }
}
