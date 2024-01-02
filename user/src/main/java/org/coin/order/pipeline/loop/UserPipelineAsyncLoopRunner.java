package org.coin.order.pipeline.loop;

import lombok.RequiredArgsConstructor;
import org.coin.order.pipeline.task.ProcessedOrderHandlerAsyncLoop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPipelineAsyncLoopRunner implements CommandLineRunner {
    private final ProcessedOrderHandlerAsyncLoop processedOrderHandlerAsyncLoop;
    @Value("${module.user.thread-pool.handler}")
    private int handlerThreadN;

    @Override
    public void run(String... args) {
        processedOrderHandlerAsyncLoop.runAsyncLoop(handlerThreadN);
    }
}
