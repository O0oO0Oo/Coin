package org.coin.trade.pipeline.eventloop.queue;

import org.coin.trade.pipeline.eventloop.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

/**
 * 읽은 주문 데이터 전처리 이벤트큐
 */
@Component("writerEventQueue")
public class WriteEventQueue extends AbstractEventQueue {
    @Autowired
    protected WriteEventQueue(
            @Qualifier("writeEventBlockingQueue") BlockingQueue<Event> events) {
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