package org.coin.trade.pipeline.eventloop.event;

public abstract class AbstractEvent<I> implements Event<I> {
    protected I data;

    protected AbstractEvent(I data) {
        this.data = data;
    }

    public I getData() {
        return data;
    }

    public void setData(I data) {
        this.data = data;
    }
}