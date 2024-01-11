package org.coin.trade.pipeline.eventloop.queue;

import org.coin.trade.pipeline.eventloop.event.Event;

import java.util.concurrent.BlockingQueue;

public abstract class AbstractEventQueue implements EventQueue {
    protected final BlockingQueue<Event> events;

    protected AbstractEventQueue(BlockingQueue<Event> events) {
        this.events = events;
    }
}