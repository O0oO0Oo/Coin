package org.coin.trade.pipeline.eventloop.loop;

/**
 * 이벤트 루프
 * 루프를 돌면서 이벤트 큐에서 작업을 가져와 이벤트 핸들러로 처리한다.
 */
public interface EventLoop {
    /**
     * 이벤트 루프 스타트
     */
    void start();

    /**
     * 이벤트 루프 종료
     */
    void stop();
}
