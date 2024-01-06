package org.coin.trade.pipeline;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

class PipelineCompletableFutureTest {
    private static final Logger logger = LoggerFactory.getLogger(PipelineSynchronizerTest.class);

    @Test
    @DisplayName("Thread-id should be the same.")
    void should_threadIdSame_when_1000ExecuteCF() {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        AtomicInteger countAll = new AtomicInteger();
        AtomicInteger countTrue = new AtomicInteger();
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

        // when
        for (int i = 0; i < 1000; i++) {
            CompletableFuture<Void> cf = CompletableFuture.supplyAsync(() -> {

                        String name = Thread.currentThread().getName();
                        return name;
                    }, executorService)
                    .thenAccept(name -> {
                        countAll.incrementAndGet();
                        if (name.equals(Thread.currentThread().getName())) {
                            countTrue.incrementAndGet();
                        }
                    });

            completableFutures.add(cf);
        }

        completableFutures.forEach(CompletableFuture::join);

        // then
        Assertions.assertEquals(countAll.get(), countTrue.get());
    }

    @Test
    @DisplayName("An error occurred then jump to handle")
    void should_jumpHandle_when_exception() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        CompletableFuture
                .supplyAsync(() -> {
                    String name = Thread.currentThread().getName();
                    boolean nextBoolean = ThreadLocalRandom.current().nextBoolean();
                    if (nextBoolean) {
                        throw new RuntimeException("throw");
                    }
                    return name;
                }, executorService)
                .thenApply(result -> {
                    logger.info("done.");
                    return result;
                })
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("An error occurred. result : {}", result);
                    }
                    return result;
                });
    }

    private ExecutorService threadPool = Executors.newFixedThreadPool(5);

    @Test
    @DisplayName("async recursion")
    void async_recursion_test() throws InterruptedException {
        asyncRecursion();

        Thread.sleep(100990);
    }

    void asyncRecursion() {
        CompletableFuture
                .runAsync(this::asyncRecursion
                ,threadPool)
                .thenRun(this::asyncRecursion)
        ;
    }

    @Test
    @DisplayName("normal recursion")
    void normal_recursion_test() {
        normalRecursion(5, 1000);
    }
    void normalRecursion(int i, int stop) {
        if (i == stop) {
            logger.info("stacktrace size : {}",Thread.currentThread().getStackTrace().length);
            return;
        }
        normalRecursion(i++, stop);
    }
}