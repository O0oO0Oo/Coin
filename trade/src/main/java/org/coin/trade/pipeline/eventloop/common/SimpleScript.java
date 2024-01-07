package org.coin.trade.pipeline.eventloop.common;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SimpleScript<I> implements Script<I> {

    @Override
    public void run(I input, Consumer<I> onSuccess, BiConsumer<Throwable, I> onFailure) {
        try{
            onSuccess.accept(input);
        } catch (RuntimeException e) {
            onFailure.accept(e, input);
        }
    }
}
