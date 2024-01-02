package org.coin.price.listener;

import lombok.extern.slf4j.Slf4j;
import org.coin.price.event.AsyncSchedulingFailureCountEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 사이트가 멈췄을 시, 가격 정보 모듈에 문제가 있을 때, API 요청을 멈추고,
 * 거래 등록을 위해 가격을 요청하는것 또한 멈추기 위한 이벤트 리스너
 */
@Slf4j
@Component
public class PriceApiRequestFailureEventListener {
    @Value("${module.price.max-failures}")
    private int maxFailures;
    private final AtomicInteger failureCount = new AtomicInteger();

    @EventListener
    public void handleEvent(AsyncSchedulingFailureCountEvent event) {
        setMaxFailures(event.isFailure());
    }

    private void setMaxFailures(boolean isFailure) {
        failureCount.accumulateAndGet(1, (current, update) -> {
            if(!isFailure && current > 0){
                return current - update;
            }

            if (current == maxFailures) {
                log.error("Price request module reached max failures");
                return current;
            }

            if(isFailure){
                return current + update;
            }

            return current;
        });
    }
}
