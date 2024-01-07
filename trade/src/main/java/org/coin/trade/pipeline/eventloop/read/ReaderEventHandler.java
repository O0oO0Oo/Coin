package org.coin.trade.pipeline.eventloop.read;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.coin.trade.pipeline.eventloop.common.AbstractEventHandler;
import org.coin.trade.pipeline.eventloop.common.Event;
import org.coin.trade.pipeline.eventloop.common.Script;
import org.coin.trade.pipeline.eventloop.common.SimpleScript;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 이벤트 처리
 * 1. 가격정보 데이터 이벤트 -> 레디스에서 가격에 맞는 주문 찾는 스크립트, 응답 CompletableFuture 를 WriterEventQueue 에 삽입
 *    가격정보 데이터 이벤트 -> 위의 스트립트에서 에러 발생 -> 다시 ReaderEventQueue 에 삽입
 */
@Component
@RequiredArgsConstructor
public class ReaderEventHandler extends AbstractEventHandler {
    private final ReadRedissonOrderScript readRedissonOrderScript;

    @PostConstruct
    void init() {
        eventScriptMap.put(ReadRedissonOrderEvent.class, readRedissonOrderScript);
    }

    @Override
    public void handle(Event event) {
        Script script = eventScriptMap.get(event.getClass());
        if (Objects.nonNull(script)) {
            event.trigger(script);
        }
    }
}