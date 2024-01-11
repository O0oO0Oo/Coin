package org.coin.order.config;

import lombok.RequiredArgsConstructor;
import org.coin.order.pipeline.eventloop.loop.ProcessedOrderEventLoop;
import org.coin.trade.pipeline.eventloop.loop.ReadEventLoop;
import org.coin.trade.pipeline.eventloop.loop.WriteEventLoop;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 이벤트 루프 시작.
 */
@Component
@RequiredArgsConstructor
public class LoopRunner implements CommandLineRunner {
    private final ReadEventLoop readEventLoop;
    private final WriteEventLoop writeEventLoop;
    private final ProcessedOrderEventLoop processedOrderEventLoop;

    @Override
    public void run(String... args) {
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService executorService2 = Executors.newSingleThreadExecutor();
        ExecutorService executorService3 = Executors.newSingleThreadExecutor();
        executorService1.submit(readEventLoop::start);
        executorService2.submit(writeEventLoop::start);
        executorService3.submit(processedOrderEventLoop::start);
    }
}
