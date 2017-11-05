package com.shzisg.mybatis.mapper.auto;

import com.shzisg.mybatis.mapper.concurrent.MapperFuture;
import com.shzisg.mybatis.mapper.page.Page;
import com.shzisg.mybatis.mapper.page.PageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface AutoMapper<Entity, Primary> {
    
    Entity findOne(@Param("id") Primary id);
    
    MapperFuture<Entity> findOneAsync(@Param("id") Primary id);
    
    List<Entity> findAll();
    
    MapperFuture<List<Entity>> findAllAsync(@Param("id") Primary id);
    
    List<Entity> findAllValid();
    
    MapperFuture<List<Entity>> findAllValidAsync();
    
    Page<Entity> findLimit(PageRequest request);
    
    MapperFuture<Page<Entity>> findLimitAsync(PageRequest request);
    
    Page<Entity> findLimitValid(PageRequest request);
    
    MapperFuture<Page<Entity>> findLimitValidAsync(PageRequest request);
    
    int insert(Entity entity);
    
    int insertAll(Collection<? extends Entity> entities);
    
    int updateSelective(Entity entity);
    
    int update(Entity entity);
    
    int updateSelectiveAll(Collection<? extends Entity> entities);
    
    int updateAll(List<? extends Entity> entities);
    
    int deleteOne(@Param("id") Primary id);
    
    int deleteAll(@Param("id") Collection<Primary> ids);
    
    int invalidOne(@Param("id") Primary id);
    
    int invalidAll(@Param("id") Collection<Primary> ids);
}
