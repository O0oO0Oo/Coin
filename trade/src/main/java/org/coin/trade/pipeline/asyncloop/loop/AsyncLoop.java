package org.coin.trade.pipeline.asyncloop.loop;

public interface AsyncLoop {
    void runAsyncLoop(int count);
    void stopAsyncLoop();
}