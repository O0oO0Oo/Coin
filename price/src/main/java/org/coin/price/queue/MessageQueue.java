package org.coin.price.queue;

public interface MessageQueue<I, O> {
    void produce(I data);
    O consume() throws InterruptedException;
}