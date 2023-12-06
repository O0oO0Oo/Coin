package org.coin.price.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.price.event.AsyncSchedulingFailureCountEvent;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;


@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncSchedulingExceptionHandler implements AsyncUncaughtExceptionHandler {
    private final ApplicationEventPublisher eventPublisher;

    // TODO : 가격 요청 모듈에 문제가 있을때 다른 모듈들에게도 알려야한다.
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Method:{}, Params:{}", method.getDeclaringClass().getSimpleName() + "." + method.getName(), Arrays.toString(params), ex);
        eventPublisher.publishEvent(AsyncSchedulingFailureCountEvent.failed());
    }
}