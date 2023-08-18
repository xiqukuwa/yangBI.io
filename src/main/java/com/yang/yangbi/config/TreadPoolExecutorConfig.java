package com.yang.yangbi.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class TreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        ThreadFactory threadFactory = new ThreadFactory() {
              private int count = 1;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count++;
                return thread;
            }
        };

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,4,100,TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),threadFactory);
        //线程池参数
//        (int corePoolSize,核心线程数，正常情况下系统可以同时工作的线程数
//        int maximumPoolSize,最大线程数，极限情况下我们的线程池有多少个线程数
//        long keepAliveTime,空闲线程存活时间，空闲线程的存活时间
//        TimeUnit unit,闲线程的存活时间
//        BlockingQueue<Runnable> workQueue,工作队列。同于给线程任务的任务队列
//        ThreadFactory threadFactory,线程工厂，生产线程
//        RejectedExecutionHandler handler) 拒绝策略，当线程满了的时候采取什么样的措施,比如抛异常
        return threadPoolExecutor;
    }
}
