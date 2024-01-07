package org.coin.trade.pipeline.asyncloop.loop;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 비동기 스케줄드 루프 추상 클래스 입니다.
 * <p>
 * 1. O processResult(I result) 결과 처리 메서드
 * <p>
 * 2. doConcurrencyLevelControl(O result) 다른 스레드와 동기화 조정이 필요할 때 동작을 정의하는 메서드
 * <p>
 * 3. doHandlerError(Throwable throwable) 예외 발생 처리 메서드
 * 4. shouldStopAsyncLoop() 예외 발생했을때 멈춰야하는 조건을 정의
 * <p>
 * 네 가지를 구현해야 합니다.
 * @param <I> Supplier 에서 반환하는 데이터의 타입
 * @param <O> 이전의 결과를 바탕으로 동시성을 조정할 데이터의 타입
 */
@Slf4j
public abstract class AbstractAsyncScheduledLoop<I, O> implements AsyncLoop {
    private ScheduledExecutorService scheduledThreadPool;
    private Supplier<I> loopSupplier;

    /**
     * loopSupplier 로부터 읽어온 결과 처리
     * @param result
     * @return
     */
    protected abstract O processResult(I result);

    /**
     * 동시성 수준 조정, 작업이 밀리면 루프를 멈추고 해결되면 재시작하기 위함.
     */
    protected abstract CompletableFuture<Void> doConcurrencyLevelControl(O result);
    private CompletableFuture<Void> concurrencyLevelControl(O result){
        return doConcurrencyLevelControl(result);
    }

    /**
     * 예외 처리
     * @param throwable
     * @return
     */
    protected abstract Void doHandlerError(Throwable throwable);
    /**
     * 예외 발생시 루프를 정지할 수 있는 조건을 정의
     * @return true 는 루프 정지, false 는 루프 계속 동작
     */
    protected abstract boolean shouldStopAsyncLoop();
    private Void handlerError(Throwable throwable){
        try {
            return doHandlerError(throwable);
        }
        finally {
            if(shouldStopAsyncLoop()) {
                stopAsyncLoop();
            }
        }
    }


    /**
     * @param period 실행 주기
     */
    @Override
    public void runAsyncLoop(int period) {
        scheduledThreadPool.scheduleAtFixedRate(this::asyncLoop, 0, period, TimeUnit.MILLISECONDS);
    }

    private void asyncLoop() {
        CompletableFuture<I> cf = CompletableFuture.supplyAsync(
                loopSupplier, scheduledThreadPool
        );

        cf
                .thenApply(this::processResult)
                .thenAccept(this::concurrencyLevelControl)
                .exceptionally(this::handlerError);
    }

    /**
     * 루프 정지
     * TODO : 언제 정지해야할까?
     */
    @Override
    public void stopAsyncLoop() {
        log.info("ThreadPool shutdown.");
        scheduledThreadPool.shutdown();
        try {
            if (!scheduledThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduledThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduledThreadPool.shutdownNow();
        }
    }

    protected void setScheduledThreadPool(ScheduledExecutorService scheduledThreadPool) {
        this.scheduledThreadPool = scheduledThreadPool;
    }

    protected void setLoopSupplier(Supplier<I> loopSupplier) {
        this.loopSupplier = loopSupplier;
    }
}
