package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.executor.keygen.KeyGenerator;

import java.util.HashMap;
import java.util.Map;

public class IdGeneratorFactory {
  private static Map<String, KeyGenerator> generatorMap = new HashMap<>();
  
  static {
    generatorMap.put("uuid", new UuidGenerator());
  }
  
  public static void setGenerator(String key, KeyGenerator generator) {
    generatorMap.put(key, generator);
  }
  
  public static KeyGenerator getGenerator(String key) {
    return generatorMap.get(key);
  }
}
