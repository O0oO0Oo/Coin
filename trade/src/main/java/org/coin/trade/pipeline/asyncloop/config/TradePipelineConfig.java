package org.coin.trade.pipeline.asyncloop.config;

import io.netty.util.concurrent.FastThreadLocal;
import org.coin.price.dto.CryptoCoin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class TradePipelineConfig {

    @Value("${module.trade.thread-pool.reader}")
    private int readerMainThreadN;
    @Value("${module.trade.thread-pool.reader-swap}")
    private int readerSwapThreadN;
    @Value("${module.trade.thread-pool.writer}")
    private int writerMainThreadN;
    @Value("${module.trade.thread-pool.writer-swap}")
    private int writerSwapThreadN;

    @Bean("pipelineSynchronizer")
    public Phaser phaser() {
        return new Phaser(5);
    }

    @Bean("readerRecursionThreadPool")
    public Pair<ExecutorService, ExecutorService> readThreadPool() {
        return Pair.of(Executors.newFixedThreadPool(readerMainThreadN), Executors.newScheduledThreadPool(readerSwapThreadN));
    }

    @Bean("writerThreadPool")
    public Pair<ExecutorService, ExecutorService> writerThreadPool() {
        return Pair.of(Executors.newFixedThreadPool(writerMainThreadN), Executors.newFixedThreadPool(writerSwapThreadN));
    }

    @Bean("readerFastThreadLocal")
    public FastThreadLocal<List<CryptoCoin>> fastThreadLocal() {
        return new FastThreadLocal<>();
    }

    @Bean("readerScheduledThreadPool")
    public ScheduledThreadPoolExecutor readerScheduledThreadPool() {
        return new ScheduledThreadPoolExecutor(readerMainThreadN);
    }
}
