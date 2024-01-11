package org.coin.trade.pipeline.eventloop.handler;

import org.coin.trade.pipeline.eventloop.event.Event;

/**
 * 이벤트 루프가 호출할 핸들러
 * 핸들러에 이벤트와 해당 이벤트가 호출할 스크립트가 매칭되어야 한다.
 */
public interface EventHandler {
    /**
     * 이벤트에 대해 특정 작업을 한다.
     * @param event
     */
    void handle(Event event);
}