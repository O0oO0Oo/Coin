package org.coin.trade.queue;

import org.coin.price.queue.MessageQueue;
import org.coin.trade.dto.pipeline.async.writer.WriteOrderDto;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ProcessedOrderMessageBlockingQueue implements MessageQueue<WriteOrderDto, WriteOrderDto> {
    private LinkedBlockingQueue<WriteOrderDto> blockingQueue = new LinkedBlockingQueue();


    @Override
    public void produce(WriteOrderDto data) {
        blockingQueue.add(data);
    }

    @Override
    public WriteOrderDto consume() throws InterruptedException {
        return blockingQueue.take();
    }
}
