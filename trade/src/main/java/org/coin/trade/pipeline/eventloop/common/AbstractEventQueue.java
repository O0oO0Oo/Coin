package org.coin.trade.pipeline.eventloop.common;

import java.util.concurrent.BlockingQueue;

public abstract class AbstractEventQueue implements EventQueue {
    protected final BlockingQueue<Event> events;

    protected AbstractEventQueue(BlockingQueue<Event> events) {
        this.events = events;
    }
}