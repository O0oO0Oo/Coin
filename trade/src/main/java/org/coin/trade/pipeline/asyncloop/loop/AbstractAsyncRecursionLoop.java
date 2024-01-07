package org.coin.trade.pipeline.asyncloop.loop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 비동기 루프 추상 클래스 입니다.
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
public abstract class AbstractAsyncRecursionLoop<I, O> implements AsyncLoop {
    private ExecutorService mainThreadPool;
    private ExecutorService swapThreadPool;
    private Supplier<I> loopSupplier;

    private final AtomicBoolean atomicChanger = new AtomicBoolean();
    private final Map<Boolean, ExecutorService> threadPoolMap = new HashMap<>();

    @Value("${module.thread-pool.stack-trace-size}")
    private int stackTraceSize;

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
        checkStackTraceThenSwapThreadPool();
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
                checkStackTraceThenSwapThreadPool();
            }
        }
    }

    /**
     * Stack Trace 사이즈 체크, 사이즈가 stackTraceSize 넘어가면 다른 스레드풀로 넘기기.
     */
    private void checkStackTraceThenSwapThreadPool() {
        if (Thread.currentThread().getStackTrace().length < stackTraceSize) {
            asyncLoop();
        } else {
            CompletableFuture.runAsync(this::asyncLoop, threadPoolMap.get(atomicChanger.getAndSet(!atomicChanger.get())));
        }
    }


    /**
     * @param count 시작 루프의 수를 결정
     */
    @Override
    public void runAsyncLoop(int count) {
        threadPoolMap.put(Boolean.TRUE, mainThreadPool);
        threadPoolMap.put(Boolean.FALSE, swapThreadPool);
        for (int i = 0; i < count; i++) {
            asyncLoop();
        }
    }

    private void asyncLoop() {
        CompletableFuture<I> cf = CompletableFuture.supplyAsync(
                loopSupplier, mainThreadPool
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
        mainThreadPool.shutdown();
        try {
            if (!mainThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                mainThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mainThreadPool.shutdownNow();
        }
    }

    public void setSwapThreadPool(ExecutorService swapThreadPool) {
        this.swapThreadPool = swapThreadPool;
    }

    protected void setMainThreadPool(ExecutorService mainThreadPool) {
        this.mainThreadPool = mainThreadPool;
    }

    protected void setLoopSupplier(Supplier<I> loopSupplier) {
        this.loopSupplier = loopSupplier;
    }
}
