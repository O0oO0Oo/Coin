package org.coin.order.pipeline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class UserPipelineConfig {
    @Value("${module.user.thread-pool.handler}")
    private int handlerThreadN;

    @Bean("handlerThreadPool")
    public ExecutorService handlerThreadPool() {
        return Executors.newFixedThreadPool(handlerThreadN);
    }
}
