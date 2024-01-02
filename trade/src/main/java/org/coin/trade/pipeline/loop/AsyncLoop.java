package org.coin.trade.pipeline.loop;

public interface AsyncLoop {
    void runAsyncLoop(int count);
    void stopAsyncLoop();
}