package org.coin.trade.pipeline.eventloop.listener;

import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.eventloop.event.WriteOrderEvent;
import org.coin.trade.pipeline.eventloop.queue.WriteEventQueue;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 쓰기 이벤트 루프 리스너, 쓰기 이벤트 큐에 이벤트 추가
 */
@Component
@RequiredArgsConstructor
public class WriteOrderEventListener {
    private final WriteEventQueue writerEventQueue;

    @EventListener
    public void handleEvent(WriteOrderEvent writeOrderEvent) {
        writerEventQueue.add(writeOrderEvent);
    }
}