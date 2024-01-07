package org.coin.trade.pipeline.eventloop.write;

import org.coin.trade.pipeline.eventloop.common.AbstractEventQueue;
import org.coin.trade.pipeline.eventloop.common.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

@Component
public class WriterEventQueue extends AbstractEventQueue {

    @Autowired
    protected WriterEventQueue(
            @Qualifier("writerEventBlockingQueue") BlockingQueue<Event> events) {
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
