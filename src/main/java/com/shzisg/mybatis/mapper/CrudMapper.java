package com.shzisg.mybatis.mapper;

import com.shzisg.mybatis.mapper.provider.CurdProvider;
import org.apache.ibatis.annotations.DeleteProvider;

public interface CrudMapper<T> {

    @DeleteProvider(type = CurdProvider.class, method = "delete")
    int delete(T record);

    @DeleteProvider(type = CurdProvider.class, method = "delete")
    int delete(String id);
}
