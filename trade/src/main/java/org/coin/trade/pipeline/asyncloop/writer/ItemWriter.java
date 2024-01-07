package org.coin.trade.pipeline.asyncloop.writer;

public interface ItemWriter <O>{
    void write(O o);
}