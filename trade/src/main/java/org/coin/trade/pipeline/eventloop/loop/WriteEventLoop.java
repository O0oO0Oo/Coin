package org.coin.trade.pipeline.eventloop.loop;

import org.coin.trade.pipeline.eventloop.handler.EventHandler;
import org.coin.trade.pipeline.eventloop.queue.EventQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 읽은 주문 데이터 전처리 위한 이벤트 루프
 */
@Component
public class WriteEventLoop extends AbstractEventLoop{
    @Autowired
    protected WriteEventLoop(@Qualifier("writerEventQueue") EventQueue eventQueue,
                             @Qualifier("tradePipelineEventHandler") EventHandler eventHandler) {
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