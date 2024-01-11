package org.coin.trade.pipeline.eventloop.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.eventloop.event.Event;
import org.coin.trade.pipeline.eventloop.event.ReadOrderEvent;
import org.coin.trade.pipeline.eventloop.event.WriteOrderEvent;
import org.coin.trade.pipeline.eventloop.script.ReadOrderScript;
import org.coin.trade.pipeline.eventloop.script.Script;
import org.coin.trade.pipeline.eventloop.script.WriteOrderScript;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 이벤트 처리
 * 1. 가격정보 데이터 이벤트 -> 레디스에서 가격에 맞는 주문 찾는 스크립트, 응답 CompletableFuture 를 {@link org.coin.trade.pipeline.eventloop.queue.WriteEventQueue} 에 삽입
 *    가격정보 데이터 이벤트 -> 위의 스트립트에서 에러 발생 -> 다시 ReadEventQueue 에 삽입
 * 2. 쓰기 데이터 이벤트 -> 위에서 읽은 주문 데이터를 저장하기위해 전처리하여 
 * {@link org.coin.trade.pipeline.eventloop.queue.ProcessedOrderEventQueue} 에 넘긴다.
 *     에러 발생시 다시 WriteEventQueue 에 삽입
 */
@Component
@RequiredArgsConstructor
public class TradePipelineEventHandler extends AbstractEventHandler {
    private final ReadOrderScript readOrderScript;
    private final WriteOrderScript writeOrderScript;

    @PostConstruct
    void init() {
        eventScriptMap.put(ReadOrderEvent.class, readOrderScript);
        eventScriptMap.put(WriteOrderEvent.class, writeOrderScript);
    }

    @Override
    public void handle(Event event) {
        Script script = eventScriptMap.get(event.getClass());
        if (Objects.nonNull(script)) {
            event.trigger(script);
        }
    }
}