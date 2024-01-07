package org.coin.trade.pipeline.eventloop.common;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Script<I> {
    void run(I input, Consumer<I> onSuccess, BiConsumer<Throwable, I> onFailure);
}
