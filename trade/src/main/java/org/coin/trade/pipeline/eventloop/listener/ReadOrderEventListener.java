package org.coin.trade.pipeline.eventloop.listener;

import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.eventloop.event.ReadOrderEvent;
import org.coin.trade.pipeline.eventloop.queue.ReadEventQueue;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 읽기 이벤트 루프 리스너, 읽기 이벤트 큐에 이벤트 추가
 */
@Component
@RequiredArgsConstructor
public class ReadOrderEventListener {
    private final ReadEventQueue readEventQueue;

    @EventListener
    public void handleEvent(ReadOrderEvent readOrderEvent) {
        readEventQueue.add(readOrderEvent);
    }
}