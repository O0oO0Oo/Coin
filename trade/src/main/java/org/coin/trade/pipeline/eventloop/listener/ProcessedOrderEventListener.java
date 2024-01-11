package org.coin.trade.pipeline.eventloop.listener;

import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.eventloop.event.ProcessedOrderEvent;
import org.coin.trade.pipeline.eventloop.queue.ProcessedOrderEventQueue;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 저장을 위한 이벤트 리스너, 전처리된 이벤트 이벤트 큐에 추가
 */
@Component
@RequiredArgsConstructor
public class ProcessedOrderEventListener {
    private final ProcessedOrderEventQueue processedOrderEventQueue;

    @EventListener
    public void handleEvent(ProcessedOrderEvent processedOrderEvent) {
        processedOrderEventQueue.add(processedOrderEvent);
    }
}