package org.coin.price.dto;

import org.coin.price.event.PriceMessageProduceEvent;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CurrentPriceTest {
    private ExecutorService executorService;
    private EasyRandom generator;
    private ArrayList<String> coinName = new ArrayList<>();
    private CurrentPrice currentPrice = new CurrentPrice();

    @BeforeEach
    void beforeEach() {
        executorService = Executors.newScheduledThreadPool(100);

        // 테스트를 위한 EasyRandom 설정
        coinName.addAll(List.of("BTC", "ETH", "ETC"));
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(FieldPredicates.named("closing_price"), () -> String.valueOf(generator.nextDouble(10, 10000)))
                .randomize(String.class, () -> coinName.get(generator.nextInt(0, 3)))
                .randomize(FieldPredicates.named("timestamp"), System::currentTimeMillis);

        generator = new EasyRandom(parameters);
    }

    @Test
    @DisplayName("setCurrentPrice - success - thread-safe")
    void should_setCurrentPriceThreadSafe_when_validRequest10000() throws InterruptedException {
        Long timestamp = 0L;
        Map<String, PriceApiRequest.PriceData> latestTimestampEvent = null;
        for (int i = 0; i < 10000; i++) {
            // given
            
            // timestamp 가 같은 경우를 방지
            Thread.sleep(1);
            PriceApiRequest request = generator.nextObject(PriceApiRequest.class);
            PriceMessageProduceEvent event = PriceMessageProduceEvent.of(request);

            if (timestamp <= event.timestamp()) {
                timestamp = event.timestamp();
                latestTimestampEvent = event.priceDataMap();
            }

            // when
            executorService.submit(() -> {
                try {
                    Thread.sleep(generator.nextInt(700));
                    currentPrice.setCurrentPrice(event);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(60, TimeUnit.SECONDS), "timeout.");

        // then
        Long finalTimestamp = timestamp;
        Map<String, PriceApiRequest.PriceData> finalLatestTimestampEvent = latestTimestampEvent;
        assertAll(
                "CurrentPrice setCurrentPrice Test",
                () -> assertEquals(
                        finalTimestamp,
                        currentPrice.getTimestamp()
                ),
                () -> assertEquals(
                        finalLatestTimestampEvent.get("BTC").getClosing_price(),
                        currentPrice.getCurrentPrice("BTC"),
                        "BTC Event.getClosing_price() must be same currentPrice"
                ),
                () -> assertEquals(
                        finalLatestTimestampEvent.get("ETH").getClosing_price(),
                        currentPrice.getCurrentPrice("ETH"),
                        "ETH Event.getClosing_price() must be same currentPrice"
                ),
                () -> assertEquals(
                        finalLatestTimestampEvent.get("ETC").getClosing_price(),
                        currentPrice.getCurrentPrice("ETC"),
                        "ETC Event.getClosing_price() must be same currentPrice"
                )
        );
    }
}