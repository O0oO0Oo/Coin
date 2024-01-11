package org.coin.trade.pipeline.eventloop.event;

import org.coin.trade.pipeline.eventloop.script.Script;

/**
 * 이벤트 루프에 들어갈 작업 데이터
 * @param <I>
 */
public interface Event<I> {
    /**
     * 이벤트에 대해 스크립트를 실행한다.
     * @param script
     */
    void trigger(Script<I> script);
}