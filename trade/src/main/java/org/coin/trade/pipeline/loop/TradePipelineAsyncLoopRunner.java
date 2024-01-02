package org.coin.trade.pipeline.loop;

import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.reader.RedissonOrderReaderAsyncLoop;
import org.coin.trade.pipeline.writer.ProcessedOrderWriterAsyncLoop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradePipelineAsyncLoopRunner implements CommandLineRunner {
    private final RedissonOrderReaderAsyncLoop redissonOrderReaderAsyncLoop;
    private final ProcessedOrderWriterAsyncLoop processedOrderWriterAsyncLoop;

    @Value("${module.trade.thread-pool.reader}")
    private int readerThreadN;
    @Value("${module.trade.thread-pool.writer}")
    private int writerThreadN;

    /**
     * 읽기/쓰기 루프 실행
     */
    @Override
    public void run(String... args) {
        redissonOrderReaderAsyncLoop.runAsyncLoop(readerThreadN);
        processedOrderWriterAsyncLoop.runAsyncLoop(writerThreadN);
    }
}
