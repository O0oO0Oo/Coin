package org.coin.trade.pipeline.asyncloop.loop;

import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.asyncloop.reader.RedissonOrderReaderAsyncScheduledLoop;
import org.coin.trade.pipeline.asyncloop.writer.ProcessedOrderWriterAsyncRecursionLoop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
public class TradePipelineAsyncLoopRunner implements CommandLineRunner {
    private final RedissonOrderReaderAsyncScheduledLoop redissonOrderReaderAsyncScheduledLoop;
    private final ProcessedOrderWriterAsyncRecursionLoop processedOrderWriterAsyncLoop;

    @Value("${module.trade.thread-pool.reader-period}")
    private int readerPeriod;
    @Value("${module.trade.thread-pool.writer}")
    private int writerThreadN;

    /**
     * 읽기/쓰기 루프 실행
     */
    @Override
    public void run(String... args) {
        redissonOrderReaderAsyncScheduledLoop.runAsyncLoop(readerPeriod);
        processedOrderWriterAsyncLoop.runAsyncLoop(writerThreadN);
    }
}
