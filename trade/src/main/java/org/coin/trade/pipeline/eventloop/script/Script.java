package org.coin.trade.pipeline.eventloop.script;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 이벤트가 실행 할 작업 정의
 * @param <I>
 */
public interface Script<I> {
    void run(I input, Consumer<I> onSuccess, BiConsumer<Throwable, I> onFailure);
}
