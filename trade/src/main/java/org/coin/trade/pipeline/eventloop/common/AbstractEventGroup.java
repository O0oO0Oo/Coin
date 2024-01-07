package org.coin.trade.pipeline.eventloop.common;

import org.coin.trade.pipeline.eventloop.common.EventLoop;
import org.coin.trade.pipeline.eventloop.common.EventLoopGroup;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class AbstractEventGroup implements EventLoopGroup {
    private final ArrayList<EventLoop> eventLoops = new ArrayList<>();
    private final ThreadPoolExecutor threadPoolExecutor;

    protected AbstractEventGroup(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void register(EventLoop eventLoop) {
        threadPoolExecutor.execute(eventLoop::start);
        eventLoops.add(eventLoop);
    }
}