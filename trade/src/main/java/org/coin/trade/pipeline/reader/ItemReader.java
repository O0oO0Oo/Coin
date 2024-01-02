package org.coin.trade.pipeline.reader;

/**
 * @param <I> type to be read
 */
public interface ItemReader<I> {
    I read();
}