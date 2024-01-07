package org.coin.trade.pipeline.eventloop.common;

import java.util.Optional;

public interface EventQueue {
    Optional<Event> next() throws InterruptedException;
    void add(Event event);
}