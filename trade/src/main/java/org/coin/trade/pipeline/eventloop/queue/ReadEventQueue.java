package org.coin.trade.pipeline.eventloop.queue;

import org.coin.trade.pipeline.eventloop.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

/**
 * 주문을 읽기 위한 이벤트 큐
 */
@Component
public class ReadEventQueue extends AbstractEventQueue {
    @Autowired
    protected ReadEventQueue(@Qualifier("readEventBlockingQueue") BlockingQueue<Event> events) {
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