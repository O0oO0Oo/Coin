package org.coin.trade.pipeline.eventloop.config;

import lombok.RequiredArgsConstructor;
import org.coin.price.queue.PriceMessageWindowBlockingQueue;
import org.coin.trade.pipeline.eventloop.event.Event;
import org.coin.trade.pipeline.eventloop.event.ReadOrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class EventLoopConfig {
    private final PriceMessageWindowBlockingQueue messageWindowBlockingQueue;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${module.trade.reader.period}")
    private int period;

    /**
     * 각 이벤트 큐 설정
     */
    @Bean("readEventBlockingQueue")
    public BlockingQueue<Event> readEventQueue() {
        return new LinkedBlockingDeque<>();
    }

    @Bean("writeEventBlockingQueue")
    public BlockingQueue<Event> writeEventQueue() {
        return new LinkedBlockingDeque<>();
    }

    @Bean("processedEventBlockingQueue")
    public BlockingQueue<Event> processedEventQueue() {
        return new LinkedBlockingDeque<>();
    }

    /**
     * 코인 가격에 맞는 주문을 처리하기 위해 설정한 주기로 가격 데이터를 이벤트 큐에 넣음
     */
    @Bean
    public void runEventProvider() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                () ->  eventPublisher.publishEvent(new ReadOrderEvent(messageWindowBlockingQueue.consume())
                ), 1000, period, TimeUnit.MILLISECONDS);
    }
}