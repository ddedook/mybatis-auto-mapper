package com.shzisg.mybatis.mapper.auto;

import org.apache.ibatis.executor.keygen.KeyGenerator;

import java.util.HashMap;
import java.util.Map;

public class MapperConfig {
    private static Map<String, KeyGenerator> generatorMap = new HashMap<>();
    private static String delFlag = "delFlag";
    
    static {
        generatorMap.put("uuid", new UuidGenerator());
    }
    
    public static void setGenerator(String key, KeyGenerator generator) {
        generatorMap.put(key, generator);
    }
    
    public static KeyGenerator getGenerator(String key) {
        return generatorMap.get(key);
    }
    
    public static String getDelFlag() {
        return delFlag;
    }
    
    public static void setDelFlag(String delFlag) {
        MapperConfig.delFlag = delFlag;
    }
}
