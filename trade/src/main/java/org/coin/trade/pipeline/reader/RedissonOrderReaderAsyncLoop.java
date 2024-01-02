package org.coin.trade.pipeline.reader;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.trade.dto.pipeline.reader.ReadOrderDto;
import org.coin.trade.pipeline.loop.AbstractAsyncLoop;
import org.coin.trade.queue.PipelineReaderBlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

@Slf4j
@Component
public class RedissonOrderReaderAsyncLoop extends AbstractAsyncLoop<ReadOrderDto, Boolean> {
    private final FastThreadLocal<List<CryptoCoin>> readerThreadLocal;
    private final Phaser phaser;
    private final PipelineReaderBlockingQueue messageQueue;
    private final FastThreadLocal<Integer> retryThreadLocal = new FastThreadLocal<>();
    @Value("${module.trade.reader.retry-limit}")
    private int readerRetryLimit;
    @Value("${module.trade.rate-limit}")
    private int rateLimit;

    @Autowired
    public RedissonOrderReaderAsyncLoop(@Qualifier("readerFastThreadLocal") FastThreadLocal<List<CryptoCoin>> readerThreadLocal,
                                        @Qualifier("pipelineSynchronizer") Phaser phaser,
                                        @Qualifier("readerThreadPool") ExecutorService threadPool,
                                        RedissonOrderReader reader,
                                        PipelineReaderBlockingQueue blockingQueue) {
        this.readerThreadLocal = readerThreadLocal;
        this.phaser = phaser;
        this.setThreadPool(threadPool);
        this.setLoopSupplier(reader.getReadSupplier());
        this.messageQueue = blockingQueue;
    }

    @Override
    protected Boolean processResult(ReadOrderDto result) {
        if (Objects.isNull(result)) {
            return Boolean.TRUE;
        }

        if(isResultEmptyThenUnlock(result)){
            return Boolean.TRUE;
        }

        this.messageQueue.produce(result);
        return Boolean.FALSE;
    }

    private boolean isResultEmptyThenUnlock(ReadOrderDto result) {
        if (Objects.isNull(result.orderSortedSetDtoList()) ||
                result.orderSortedSetDtoList().isEmpty()
        ) {
            if(result.lock().isLocked()) {
                result.lock().unlock();
            }
            log.info(result.lock().getName() + " is unlocked.");
            return true;
        }
        return false;
    }

    @Override
    protected CompletableFuture<Void> doConcurrencyLevelControl(Boolean isEmptyResult) {
        // 성공 시 이전의 데이터 삭제
        readerThreadLocal.remove();

        if (Boolean.TRUE.equals(isEmptyResult)) {
            return null;
        }

        if (phaser.getRegisteredParties() > rateLimit) {
            log.info("stop reader async loop.");
            phaser.arriveAndAwaitAdvance();
            log.info("start reader async loop.");
        }

        // parties++
        phaser.register();
        return null;
    }

    /**
     * 에러 처리
     * TODO : 횟수 제한을 넘는 에러는 어떻게 처리해야 할까?
     *        그러한 경우가 발생할까?
     *        단순 로깅?
     *        로깅 후 다음으로 넘어가야하나, 시간을 늘려가며 대기한 후에 재시작해야하나.
     */
    @Override
    protected Void doHandlerError(Throwable throwable) {
        Integer retryCount = retryThreadLocal.getIfExists();

        if(Objects.isNull(retryCount)){
            retryThreadLocal.set(1);
        }
        else {
            retryThreadLocal.set(++retryCount);
        }

        // 횟수 제한 초과, 로깅 후 다음 읽기로 넘어감
        if (retryCount > readerRetryLimit) {
            List<CryptoCoin> cryptoCoins = readerThreadLocal.get();
            retryThreadLocal.remove();
            log.error("The number of retries was exceeded due to ({}) when processing {}", throwable.getMessage(), cryptoCoins);
        }

        return null;
    }

    @Override
    protected boolean shouldStopAsyncLoop() {
        return false;
    }
}
