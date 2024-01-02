package org.coin.trade.queue;

import org.coin.price.queue.MessageQueue;
import org.coin.trade.dto.pipeline.reader.ReadOrderDto;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
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