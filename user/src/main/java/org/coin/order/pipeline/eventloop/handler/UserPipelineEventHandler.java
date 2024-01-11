package org.coin.order.pipeline.eventloop.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.coin.order.pipeline.eventloop.script.ProcessedOrderScript;
import org.coin.trade.pipeline.eventloop.event.Event;
import org.coin.trade.pipeline.eventloop.event.ProcessedOrderEvent;
import org.coin.trade.pipeline.eventloop.handler.AbstractEventHandler;
import org.coin.trade.pipeline.eventloop.script.Script;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 유저 파이프라인 핸들러 - 전처리된 데이터 저장하기위한 핸들러
 */
@Component
@RequiredArgsConstructor
public class UserPipelineEventHandler extends AbstractEventHandler {
    private final ProcessedOrderScript processedOrderScript;

    @PostConstruct
    void init() {
        eventScriptMap.put(ProcessedOrderEvent.class, processedOrderScript);
    }

    @Override
    public void handle(Event event) {
        Script script = eventScriptMap.get(event.getClass());
        if (Objects.nonNull(script)) {
            event.trigger(script);
        }
    }
}
