package org.coin.price.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.price.exception.AsyncSchedulingExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@Slf4j
@EnableAsync
@EnableScheduling
@Configuration
@RequiredArgsConstructor
public class AsyncSchedulingConfiguration implements AsyncConfigurer {
    @Value("${module.price.rps}")
    private int rps;
    private final AsyncSchedulingExceptionHandler asyncSchedulingExceptionHandler;

    /**
     * Thread = rps x response time
     * rps : 초당 4번 요청
     * response time : PriceRequestTask 는 1초 미만의 응답시간(88 ~ 771ms), 1초로 설정
     */
    @Override
    @Bean(name = "priceRequestTaskExecutor")
    public Executor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(rps);
        executor.setThreadNamePrefix("pool-price-thread-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncSchedulingExceptionHandler;
    }
}
