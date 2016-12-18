package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface Mapper<Entity> {
  
  Entity findOne(@Param("id") String id);
  
  List<Entity> findAll();
  
  void insert(Entity entity);
  
  void insertAll(List<Entity> entities);
  
  void update(Entity entity);
  
  void deleteOne(@Param("id") String id);
}
