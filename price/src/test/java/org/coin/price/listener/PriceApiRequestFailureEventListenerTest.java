package org.coin.price.listener;

import org.coin.price.event.AsyncSchedulingFailureCountEvent;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PriceApiRequestFailureEventListenerTest {

    private ExecutorService executorService;
    private EasyRandom generator;
    private PriceApiRequestFailureEventListener priceApiRequestFailureEventListener;
    private int maxFailures = 10;

    @BeforeEach
    void beforeEach() {
        priceApiRequestFailureEventListener = new PriceApiRequestFailureEventListener();
        ReflectionTestUtils.setField(priceApiRequestFailureEventListener, "maxFailures", maxFailures);
        executorService = Executors.newScheduledThreadPool(100);
        generator = new EasyRandom();
    }

    @Test
    @DisplayName("event")
    void should_handleEventThreadSafe_when_validEvent10000() throws InterruptedException {
        AtomicInteger result = (AtomicInteger) ReflectionTestUtils.getField(priceApiRequestFailureEventListener, "failureCount");
        List<AsyncSchedulingFailureCountEvent> eventList =
                List.of(
                        AsyncSchedulingFailureCountEvent.success(),
                        AsyncSchedulingFailureCountEvent.failed()
                );

        for (int i = 0; i < 1000000; i++) {
            // given
            AsyncSchedulingFailureCountEvent event = eventList.get(generator.nextInt(0, 2));

            // when
            executorService.submit(() -> {
                priceApiRequestFailureEventListener.handleEvent(event);
            });

            // then
            if (result.get() < 0) {
                fail("failureCount must be greater than 0.");
            }

            if (result.get() > maxFailures) {
                fail("failureCount must be less than 0.");
            }
        }

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(60, TimeUnit.SECONDS), "timeout.");
    }
}