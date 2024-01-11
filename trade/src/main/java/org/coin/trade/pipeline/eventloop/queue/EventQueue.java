package org.coin.trade.pipeline.eventloop.queue;

import org.coin.trade.pipeline.eventloop.event.Event;

import java.util.Optional;

public interface EventQueue {
    /**
     * @return 큐에서 다음 이벤트 값을 획득
     * @throws InterruptedException
     */
    Optional<Event> next() throws InterruptedException;

    /**
     * 이벤트 큐에 이벤트 추가
     * @param event
     */
    void add(Event event);
}