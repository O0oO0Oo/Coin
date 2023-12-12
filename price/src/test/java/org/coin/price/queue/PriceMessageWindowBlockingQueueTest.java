package org.coin.price.queue;

import org.coin.price.dto.CryptoCoin;
import org.coin.price.dto.CryptoCoinComparator;
import org.coin.price.dto.PriceApiRequest;
import org.coin.price.event.PriceMessageProduceEvent;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PriceMessageWindowBlockingQueueTest {
    private ExecutorService produceExecutorService;
    private ExecutorService consumeExecutorService;
    private EasyRandom generator;
    private PriceMessageWindowBlockingQueue priceMessageBlockingQueue = new PriceMessageWindowBlockingQueue();
    private ConcurrentHashMap<String, PriorityBlockingQueue<CryptoCoin>> priceHashMapPriorityQueue = new ConcurrentHashMap<>();
    private ArrayList<String> coinName = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        produceExecutorService = Executors.newScheduledThreadPool(100);
        consumeExecutorService = Executors.newScheduledThreadPool(100);

        coinName.addAll(List.of("BTC", "ETH", "ETC"));
        priceHashMapPriorityQueue.put(coinName.get(0), new PriorityBlockingQueue<>(100, new CryptoCoinComparator()));
        priceHashMapPriorityQueue.put(coinName.get(1), new PriorityBlockingQueue<>(100, new CryptoCoinComparator()));
        priceHashMapPriorityQueue.put(coinName.get(2), new PriorityBlockingQueue<>(100, new CryptoCoinComparator()));
        ReflectionTestUtils.setField(priceMessageBlockingQueue, "priceHashMapPriorityQueue", priceHashMapPriorityQueue);
        ReflectionTestUtils.setField(priceMessageBlockingQueue, "coins", coinName);
        ReflectionTestUtils.setField(priceMessageBlockingQueue,"windowSize", 5);
    }

    @Test
    @DisplayName("consume - success - 5 window-size, 4 types price")
    void should_consumeThreadSafe_when_validRequest10000WindowSize5PriceType4() throws InterruptedException {
        // given
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(FieldPredicates.named("closing_price"), () -> String.valueOf(generator.nextInt(10, 14)))
                .randomize(String.class, () -> coinName.get(generator.nextInt(0, 3)))
                .randomize(FieldPredicates.named("timestamp"), System::currentTimeMillis);

        generator = new EasyRandom(parameters);

        int size = 0;
        for (int i = 0; i < 10000; i++) {
            PriceApiRequest request = generator.nextObject(PriceApiRequest.class);
            PriceMessageProduceEvent event = PriceMessageProduceEvent.of(request);

            size += event.priceDataMap().keySet().size();

            produceExecutorService.submit(() -> {
                priceMessageBlockingQueue.produce(event);
            });
        }

        produceExecutorService.shutdown();
        assertTrue(produceExecutorService.awaitTermination(60, TimeUnit.SECONDS), "timeout.");
        System.out.println("produce success, size : " + size);

        AtomicInteger actualSize = new AtomicInteger();

        // when
        for (int i = 0; i < 20; i++) {
            consumeExecutorService.submit(() -> {
                while (true) {
                    List<CryptoCoin> consume = priceMessageBlockingQueue.consume();
                    actualSize.addAndGet(consume.size());
                    if (consume.isEmpty()) {
                        break;
                    }
                }
            });
        }

        consumeExecutorService.shutdown();
        assertTrue(consumeExecutorService.awaitTermination(60, TimeUnit.SECONDS), "timeout.");

        // then
        assertAll(
                "PriceMessageWindowBlockingQueue consume test",
                () -> assertEquals(
                        12,
                        actualSize.get(),
                        "size diff"
                ),
                () -> assertEquals(
                        0,
                        priceHashMapPriorityQueue.get(coinName.get(0)).size(),
                        coinName.get(0) + " PriorityBlockingQueue must be 0"
                ),
                () -> assertEquals(
                        0,
                        priceHashMapPriorityQueue.get(coinName.get(1)).size(),
                        coinName.get(1) + " PriorityBlockingQueue must be 0"
                ),
                () -> assertEquals(
                        0,
                        priceHashMapPriorityQueue.get(coinName.get(2)).size(),
                        coinName.get(2) + " PriorityBlockingQueue must be 0"
                )
        );
    }
}