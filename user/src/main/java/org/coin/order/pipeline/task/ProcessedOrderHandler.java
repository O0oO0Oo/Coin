package org.coin.order.pipeline.task;

import lombok.RequiredArgsConstructor;
import org.coin.trade.dto.pipeline.writer.WriteOrderDto;
import org.coin.trade.queue.ProcessedOrderMessageBlockingQueue;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ProcessedOrderHandler implements Supplier<WriteOrderDto> {
    private final ProcessedOrderMessageBlockingQueue blockingQueue;

    @Override
    public WriteOrderDto get() {
        try {
            return blockingQueue.consume();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}