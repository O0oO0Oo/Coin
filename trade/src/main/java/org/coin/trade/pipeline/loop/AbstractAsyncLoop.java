package org.coin.trade.pipeline.loop;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 비동기 루프 추상 클래스 입니다.
 * <p></p>
 * 1. processResult(I result) 결과 처리 메서드
 * <p>
 * 2. doConcurrencyLevelControl() 다른 스레드와 동기화 조정이 필요할 때 동작을 정의하는 메서드
 * <p>
 * 3. doHandlerError(Throwable throwable) 예외 발생 처리 메서드
 * <p>
 * 세 가지를 구현해야 합니다.
 * @param <I> Supplier 에서 반환하는 데이터 타입
 */
@Slf4j
public abstract class AbstractAsyncLoop<I, O> implements AsyncLoop {
    private ExecutorService threadPool;
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
        CompletableFuture<Void> voidCompletableFuture = doConcurrencyLevelControl(result);
        asyncLoop();
        return voidCompletableFuture;
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
            else {
                asyncLoop();
            }
        }
    }

    /**
     * @param count 시작 루프의 수를 결정
     */
    @Override
    public void runAsyncLoop(int count) {
        for (int i = 0; i < count; i++) {
            asyncLoop();
        }
    }

    private void asyncLoop() {
        CompletableFuture<I> cf = CompletableFuture.supplyAsync(
                loopSupplier, threadPool
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
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            threadPool.shutdownNow();
        }
    }

    protected void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    protected void setLoopSupplier(Supplier<I> loopSupplier) {
        this.loopSupplier = loopSupplier;
    }
}