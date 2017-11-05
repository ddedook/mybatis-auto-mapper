package com.shzisg.mybatis.mapper.concurrent;

import java.util.concurrent.ExecutionException;

/**
 * @author harris
 */
public class MapperFuture<T> {
    
    java.util.concurrent.Future<T> future;
    
    public MapperFuture(java.util.concurrent.Future<T> future) {
        this.future = future;
    }
    
    public T get() {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("get future result failed", e);
        }
    }
}
