package com.shzisg.mybatis.mapper;

import org.apache.ibatis.session.Configuration;

public class MapperBuilder {

    public void buildMapper(Configuration configuration, Class<?> mapperInterface) {
        if (!mapperInterface.isAssignableFrom(Mapper.class)) {
            return;
        }


    }
}
