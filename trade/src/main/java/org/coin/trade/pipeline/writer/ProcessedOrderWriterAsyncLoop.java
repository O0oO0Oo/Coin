package org.coin.trade.pipeline.writer;

import lombok.extern.slf4j.Slf4j;
import org.coin.trade.pipeline.loop.AbstractAsyncLoop;
import org.coin.trade.queue.PipelineReaderBlockingQueue;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ProcessedOrderWriterAsyncLoop extends AbstractAsyncLoop<RLock, Boolean> {
    private final Phaser phaser;
    private final PipelineReaderBlockingQueue pipelineReaderBlockingQueue;
    @Value("${module.trade.rate-limit}")
    private int rateLimit;

    @Autowired
    public ProcessedOrderWriterAsyncLoop(@Qualifier("pipelineSynchronizer") Phaser phaser,
                                         @Qualifier("writerThreadPool")ExecutorService threadPool,
                                         ProcessedOrderWriter writer,
                                         PipelineReaderBlockingQueue pipelineReaderBlockingQueue){
        this.phaser = phaser;
        this.setThreadPool(threadPool);
        this.setLoopSupplier(writer.writeSupplier());
        this.pipelineReaderBlockingQueue = pipelineReaderBlockingQueue;
    }

    AtomicBoolean no = new AtomicBoolean(false);

    @Override
    protected Boolean processResult(RLock lock) {
        if (Objects.isNull(lock)) {
            return Boolean.TRUE;
        }

        // 거래 완료 락 제거
        if(lock.isLocked()) {
            log.error(lock.getName() + " is unlocked.");
            lock.unlock();
        }

        return Boolean.FALSE;
    }

    @Override
    protected CompletableFuture<Void> doConcurrencyLevelControl(Boolean isEmptyResult) {
        if (Boolean.TRUE.equals(isEmptyResult)) {
            return null;
        }

        if (phaser.getRegisteredParties() > rateLimit) {
            phaser.arrive();
        }

        if(phaser.getRegisteredParties() > 1) {
            phaser.arriveAndDeregister();
        }

        return null;
    }

    // TODO : 에러처리
    @Override
    protected Void doHandlerError(Throwable throwable) {
        return null;
    }

    @Override
    protected boolean shouldStopAsyncLoop() {
        return false;
    }
}