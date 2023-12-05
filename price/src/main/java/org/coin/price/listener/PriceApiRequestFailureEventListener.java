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
    @Value("${price.api.max-failures}")
    private int maxFailures;
    private final AtomicInteger failureCount = new AtomicInteger();

    @EventListener
    public void handleEvent(AsyncSchedulingFailureCountEvent event) {
        setMaxFailures(event.isFailure());
    }

    private void setMaxFailures(boolean isFailure) {
        if (!isFailure) {
            failureCount.decrementAndGet();
            failureCount.compareAndSet(-1, 0);
        }

        if (failureCount.get() == maxFailures) {
            log.error("Price request module reached max failures");
            // TODO : 가격데이터를 불러올 수 없으므로 거래를 진행할 수 없어 거래 등록 서비스가 중지되게 해야함
            return;
        }

        if (isFailure) {
            failureCount.incrementAndGet();
        }
    }
}
