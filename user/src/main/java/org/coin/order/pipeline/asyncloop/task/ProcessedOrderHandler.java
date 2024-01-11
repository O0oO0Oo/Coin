package org.coin.order.pipeline.asyncloop.task;

import lombok.RequiredArgsConstructor;
import org.coin.trade.dto.pipeline.async.writer.WriteOrderDto;
import org.coin.trade.pipeline.asyncloop.queue.ProcessedOrderMessageBlockingQueue;

import java.util.function.Supplier;

//@Component
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