package org.coin.trade.pipeline.eventloop.read;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.coin.price.queue.PriceMessageWindowBlockingQueue;
import org.coin.trade.pipeline.eventloop.common.EventHandler;
import org.coin.trade.pipeline.eventloop.common.EventLoop;
import org.coin.trade.pipeline.eventloop.common.SimpleEventLoop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ReaderMultithreadEventLoop implements EventLoop {
    @Value("${module.trade.thread-pool.reader}")
    private int nThreads;
    @Value("${module.trade.thread-pool.reader-period}")
    private int milliSecondsPeriod;

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor
            = new ScheduledThreadPoolExecutor(2);
    private EventLoop eventLoop;
    private final PriceMessageWindowBlockingQueue messageWindowBlockingQueue;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ReaderEventQueue readerEventQueue;
    private final ReaderEventHandler readerEventHandler;

    @PostConstruct
    void init() {
        eventLoop = new SimpleEventLoop(readerEventQueue, readerEventHandler);
    }

    @Override
    public void start() {
        // 2ms 마다 가격정보 데이터 제공
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                () ->  readerEventQueue.add(new ReadRedissonOrderEvent(messageWindowBlockingQueue.consume())
                ), 0, milliSecondsPeriod, TimeUnit.MILLISECONDS);
        // n개의 스레드풀 시작
        for (int i = 0; i < nThreads; i++) {
            threadPoolExecutor.execute(eventLoop::start);
        }
    }

    @Override
    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
        threadPoolExecutor.shutdown();
    }
}
