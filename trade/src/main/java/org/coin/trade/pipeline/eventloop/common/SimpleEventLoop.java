package org.coin.trade.pipeline.eventloop.common;

public class SimpleEventLoop extends AbstractEventLoop{
    public SimpleEventLoop(EventQueue eventQueue, EventHandler eventHandler) {
        super(eventQueue, eventHandler);
    }

    public void start() {
        while (alive.get()) {
            try {
                eventQueue.next().ifPresent(eventHandler::handle);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void stop() {
        alive.set(false);
    }
}
