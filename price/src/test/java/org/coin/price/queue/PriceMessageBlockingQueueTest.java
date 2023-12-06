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

import static org.junit.jupiter.api.Assertions.*;


class PriceMessageBlockingQueueTest {
    private ExecutorService produceExecutorService;
    private ExecutorService consumeExecutorService;
    private EasyRandom generator;
    private PriceMessageBlockingQueue priceMessageBlockingQueue = new PriceMessageBlockingQueue();
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

        // 테스트를 위한 EasyRandom 설정
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(FieldPredicates.named("closing_price"), () -> String.valueOf(generator.nextDouble(10, 10000)))
                .randomize(String.class, () -> coinName.get(generator.nextInt(0, 3)))
                .randomize(FieldPredicates.named("timestamp"), System::currentTimeMillis);
        generator = new EasyRandom(parameters);
    }

    @Test
    @DisplayName("produce - success")
    void should_produceThreadSafe_when_validRequest10000() throws InterruptedException {
        int size = 0;
        for (int i = 0; i < 10000; i++) {
            // given
            PriceApiRequest request = generator.nextObject(PriceApiRequest.class);
            PriceMessageProduceEvent event = PriceMessageProduceEvent.of(request);

            size += event.priceDataMap().keySet().size();

            // when
            produceExecutorService.submit(() -> {
                try {
                    // API 요청 받아오는 시간
                    Thread.sleep(generator.nextLong(50, 700));
                    priceMessageBlockingQueue.produce(event);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        produceExecutorService.shutdown();
        assertTrue(produceExecutorService.awaitTermination(60, TimeUnit.SECONDS), "timeout.");

        // then
        int finalSize = size;
        assertAll(
                "PriceMessageBlockingQueue produce test",
                () -> assertEquals(
                        finalSize,
                        priceHashMapPriorityQueue.get(coinName.get(0)).size() +
                                priceHashMapPriorityQueue.get(coinName.get(1)).size() +
                                priceHashMapPriorityQueue.get(coinName.get(2)).size(),
                        "Non Thread-safe"
                ),
                () -> assertEquals(
                        finalSize/3,
                        priceHashMapPriorityQueue.get(coinName.get(0)).size(),
                        "Array[0] must be " + finalSize/3
                ),
                () -> assertEquals(
                        finalSize/3,
                        priceHashMapPriorityQueue.get(coinName.get(1)).size(),
                        "Array[1] must be " + finalSize/3
                ),
                () -> assertEquals(
                        finalSize/3,
                        priceHashMapPriorityQueue.get(coinName.get(2)).size(),
                        "Array[2] must be " + finalSize/3
                )
        );
    }

    @Test
    @DisplayName("consume - success")
    void should_consumeThreadSafe_when_validRequest10000() throws InterruptedException, ExecutionException {
        // given
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

        // when
        for (int i = 0; i < size; i++) {
            consumeExecutorService.submit(() -> {
                priceMessageBlockingQueue.consume();
            });
        }

        consumeExecutorService.shutdown();
        assertTrue(produceExecutorService.awaitTermination(60, TimeUnit.SECONDS), "timeout.");

        // then
        assertAll(
                "PriceMessageBlockingQueue consume test",
                () -> assertEquals(
                        0,
                        priceHashMapPriorityQueue.get(coinName.get(0)).size(),
                        "Array[0] must be 0"
                ),
                () -> assertEquals(
                        0,
                        priceHashMapPriorityQueue.get(coinName.get(1)).size(),
                        "Array[1] must be 0"
                ),
                () -> assertEquals(
                        0,
                        priceHashMapPriorityQueue.get(coinName.get(2)).size(),
                        "Array[2] must be 0"
                )
        );
    }
}