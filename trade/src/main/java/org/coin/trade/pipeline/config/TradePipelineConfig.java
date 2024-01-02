package org.coin.trade.pipeline.config;

import io.netty.util.concurrent.FastThreadLocal;
import org.coin.price.dto.CryptoCoin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

@Configuration
public class TradePipelineConfig {

    @Value("${module.trade.thread-pool.reader}")
    private int readerThreadN;
    @Value("${module.trade.thread-pool.writer}")
    private int writerThreadN;

    @Bean("pipelineSynchronizer")
    public Phaser phaser() {
        return new Phaser(5);
    }

    @Bean("readerThreadPool")
    public ExecutorService readThreadPool() {
        return Executors.newFixedThreadPool(readerThreadN);
    }

    @Bean("writerThreadPool")
    public ExecutorService writerThreadPool() {
        return Executors.newFixedThreadPool(writerThreadN);
    }

    @Bean("readerFastThreadLocal")
    public FastThreadLocal<List<CryptoCoin>> fastThreadLocal() {
        return new FastThreadLocal<>();
    }
}
