package org.coin.order.pipeline.eventloop.loop;

import org.coin.trade.pipeline.eventloop.handler.EventHandler;
import org.coin.trade.pipeline.eventloop.loop.AbstractEventLoop;
import org.coin.trade.pipeline.eventloop.queue.EventQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 전처리된 데이터를 읽기위한 이벤트 루프
 */
@Component
public class ProcessedOrderEventLoop extends AbstractEventLoop {
    @Autowired
    protected ProcessedOrderEventLoop(@Qualifier("processedOrderEventQueue") EventQueue eventQueue,
                                      @Qualifier("userPipelineEventHandler") EventHandler eventHandler) {
        super(eventQueue, eventHandler);
    }

    @Override
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
