package org.coin.trade.pipeline.eventloop.common;

public abstract class AbstractEvent<I> implements Event<I> {
    protected I data;

    public AbstractEvent(I data) {
        this.data = data;
    }

    public I getData() {
        return data;
    }

    public void setData(I data) {
        this.data = data;
    }
}
