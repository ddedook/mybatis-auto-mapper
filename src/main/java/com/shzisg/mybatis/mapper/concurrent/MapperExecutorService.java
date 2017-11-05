package com.shzisg.mybatis.mapper.concurrent;

import java.util.concurrent.*;

/**
 * @author harris
 */
public class MapperExecutorService {
    
    private static ExecutorService executorService;
    
    public static void setExecutorService(ExecutorService executorService) {
        MapperExecutorService.executorService = executorService;
    }
    
    public static void defaultExecutor() {
        ThreadFactory namedThreadFactory = r -> {
            Thread thread = new Thread(r, "mybatis-mapper-thread");
            thread.setDaemon(true);
            return thread;
        };
        executorService = new ThreadPoolExecutor(30, 100, 5,
            TimeUnit.MINUTES, new LinkedBlockingDeque<>(1000), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
    }
    
    public static ExecutorService executorService() {
        Executors.newFixedThreadPool(10);
        return executorService;
    }
    
    @SuppressWarnings("unchecked")
    public static MapperFuture<?> submit(Callable task) {
        return new MapperFuture<>(executorService.submit(task));
    }
}
