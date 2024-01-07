package org.coin.trade.pipeline.asyncloop.reader;

/**
 * @param <I> type to be read
 */
public interface ItemReader<I> {
    I read();
}