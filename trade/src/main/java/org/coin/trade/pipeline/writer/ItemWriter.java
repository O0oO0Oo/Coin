package org.coin.trade.pipeline.writer;

public interface ItemWriter <O>{
    void write(O o);
}