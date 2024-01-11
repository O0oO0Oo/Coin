package org.coin.trade.pipeline.eventloop.queue;

import org.coin.trade.pipeline.eventloop.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

/**
 * 전처리된 주문 데이터 저장하기 위한 이벤트 큐
 * ProcessedOrderEventQueue, ProcessedOrderScript 는 User 패키지에
 */
@Component
public class ProcessedOrderEventQueue extends AbstractEventQueue {

    @Autowired
    protected ProcessedOrderEventQueue(@Qualifier("processedEventBlockingQueue") BlockingQueue<Event> events) {
        super(events);
    }

    @Override
    public Optional<Event> next() throws InterruptedException {
        return Optional.of(events.take());
    }

    @Override
    public void add(Event event) {
        events.add(event);
    }
}
