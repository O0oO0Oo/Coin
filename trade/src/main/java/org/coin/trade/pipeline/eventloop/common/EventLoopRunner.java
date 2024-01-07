package org.coin.trade.pipeline.eventloop.common;

import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.eventloop.read.ReaderMultithreadEventLoop;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventLoopRunner implements CommandLineRunner {
    private final ReaderMultithreadEventLoop eventLoop;
    @Override
    public void run(String... args) throws Exception {
        eventLoop.start();
    }
}
