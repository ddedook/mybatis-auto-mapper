package com.shzisg.mybatis.mapper.auto;

import com.shzisg.mybatis.mapper.page.Page;
import com.shzisg.mybatis.mapper.page.PageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface AutoMapper<Entity> {
    
    Entity findOne(@Param("id") String id);
    
    List<Entity> findAll();
    
    Page<Entity> findLimit(PageRequest request);
    
    int insert(Entity entity);
    
    int insertAll(List<Entity> entities);
    
    int update(Entity entity);
    
    int deleteOne(@Param("id") String id);
    
    int deleteAll(Collection<String> ids);
}
