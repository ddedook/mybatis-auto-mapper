package com.shzisg.mybatis.mapper.auto;

import com.shzisg.mybatis.mapper.page.Page;
import com.shzisg.mybatis.mapper.page.PageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface AutoMapper<Entity> {
    
    Entity findOne(@Param("id") String id);
    
    List<Entity> findAll();
    
    List<Entity> findAllValid();
    
    Page<Entity> findLimit(PageRequest request);
    
    Page<Entity> findLimitValid(PageRequest request);
    
    int insert(Entity entity);
    
    int insertAll(List<Entity> entities);
    
    int updateSelective(Entity entity);
    
    int update(Entity entity);
    
    int updateSelectiveAll(List<Entity> entities);
    
    int updateAll(List<Entity> entities);
    
    int deleteOne(@Param("id") String id);
    
    int deleteAll(@Param("id") Collection<String> ids);
    
    int invalidOne(@Param("id") String id);
    
    int invalidAll(@Param("id") Collection<String> ids);
}
