package org.coin.trade.pipeline.asyncloop.writer;

import lombok.extern.slf4j.Slf4j;
import org.coin.trade.pipeline.asyncloop.loop.AbstractAsyncRecursionLoop;
import org.coin.trade.redis.CustomOrderLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ProcessedOrderWriterAsyncRecursionLoop extends AbstractAsyncRecursionLoop<CustomOrderLock, Boolean> {
    private final Phaser phaser;
    @Value("${module.trade.rate-limit}")
    private int rateLimit;

    @Autowired
    public ProcessedOrderWriterAsyncRecursionLoop(@Qualifier("pipelineSynchronizer") Phaser phaser,
                                                  @Qualifier("writerThreadPool") Pair<ExecutorService, ExecutorService> threadPool,
                                                  ProcessedOrderWriter writer){
        this.phaser = phaser;
        this.setMainThreadPool(threadPool.getFirst());
        this.setSwapThreadPool(threadPool.getSecond());
        this.setLoopSupplier(writer.writeSupplier());
    }

    AtomicBoolean no = new AtomicBoolean(false);

    @Override
    protected Boolean processResult(CustomOrderLock lock) {
        if (Objects.isNull(lock)) {
            return Boolean.TRUE;
        }

        // 거래 완료 락 제거
        if(lock.isLocked()) {
            log.info(lock.getName() + " is unlocked.");
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
        log.error("An error occurred in the trade pipeline - writer.", throwable);
        return null;
    }

    @Override
    protected boolean shouldStopAsyncLoop() {
        return false;
    }
}