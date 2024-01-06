package org.coin.order.pipeline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class UserPipelineConfig {
    @Value("${module.user.thread-pool.handler}")
    private int handlerThreadN;

    @Value("${module.user.thread-pool.handler-swap}")
    private int handlerSwapThreadN;

    @Bean("handlerThreadPool")
    public Pair<ExecutorService, ExecutorService> handlerThreadPool() {
        return Pair.of(Executors.newFixedThreadPool(handlerThreadN),Executors.newFixedThreadPool(handlerSwapThreadN));
    }
}