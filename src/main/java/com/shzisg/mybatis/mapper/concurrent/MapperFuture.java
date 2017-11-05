package com.shzisg.mybatis.mapper.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author harris
 */
public class MapperFuture<V> implements Future<V> {
    
    java.util.concurrent.Future<V> future;
    
    public MapperFuture(java.util.concurrent.Future<V> future) {
        this.future = future;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }
    
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }
    
    @Override
    public boolean isDone() {
        return future.isDone();
    }
    
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return future.get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("get future result failed", e);
        }
    }
    
    public V get() {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("get future result failed", e);
        }
    }
}
