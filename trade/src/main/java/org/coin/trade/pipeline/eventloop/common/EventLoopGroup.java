package org.coin.trade.pipeline.eventloop.common;

public interface EventLoopGroup {
    void register(EventLoop eventLoop);
}
