package org.coin.trade.pipeline.eventloop.loop;

import org.coin.trade.pipeline.eventloop.handler.EventHandler;
import org.coin.trade.pipeline.eventloop.queue.EventQueue;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractEventLoop implements EventLoop {
    protected final AtomicBoolean alive = new AtomicBoolean(true);
    protected final EventQueue eventQueue;
    protected final EventHandler eventHandler;

    protected AbstractEventLoop(EventQueue eventQueue, EventHandler eventHandler) {
        this.eventQueue = eventQueue;
        this.eventHandler = eventHandler;
    }
}