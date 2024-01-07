package org.coin.trade.pipeline.eventloop.common;

public interface Event<I> {
    void trigger(Script<I> script);
}