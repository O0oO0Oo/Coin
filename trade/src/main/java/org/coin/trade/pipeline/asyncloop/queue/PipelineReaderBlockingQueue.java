package org.coin.trade.pipeline.asyncloop.queue;

import org.coin.price.queue.MessageQueue;
import org.coin.trade.dto.pipeline.async.reader.ReadOrderDto;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//@Component
public class PipelineReaderBlockingQueue implements MessageQueue<ReadOrderDto, ReadOrderDto> {
    private final BlockingQueue<ReadOrderDto> blockingQueue = new LinkedBlockingQueue<>();

    @Override
    public void produce(ReadOrderDto dto) {
        blockingQueue.add(dto);
    }

    @Override
    public ReadOrderDto consume() throws InterruptedException {
        return blockingQueue.take();
    }
}