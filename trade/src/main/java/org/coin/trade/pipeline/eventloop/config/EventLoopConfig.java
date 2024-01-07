package org.coin.trade.pipeline.eventloop.config;

import org.coin.trade.dto.pipeline.event.reader.ReadOrderDto;
import org.coin.trade.pipeline.eventloop.common.Event;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

@Configuration
public class EventLoopConfig {

    @Bean("readerEventBlockingQueue")
    public BlockingQueue<Event> readerEventQueue() {
        return new LinkedBlockingDeque<>();
    }

    @Bean("writerEventBlockingQueue")
    public BlockingQueue<Event> writerEventQueue() {
        return new LinkedBlockingDeque<>();
    }
}
