package org.coin.trade.pipeline.reader;

import org.coin.price.queue.PriceMessageWindowBlockingQueue;
import org.coin.trade.config.RedissonWriteToMasterReadFromReplicaConfiguration;
import org.coin.trade.dto.service.OrderDto;
import org.coin.trade.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ActiveProfiles("test")
@SpringBootTest(classes = {RedissonWriteToMasterReadFromReplicaConfiguration.class, TradeService.class, PriceMessageWindowBlockingQueue.class})
class RedissonReaderAsyncLoopOrderReaderTest {
    private static final Logger logger = LoggerFactory.getLogger(RedissonReaderAsyncLoopOrderReaderTest.class);
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private TradeService tradeService;

    private final Integer THREAD_POOL_SIZE = 5;
    private ExecutorService threadPool;
    private List<Double> priceList = List.of(1000., 2000., 3000., 4000., 5000., 6000.);
    private BlockingQueue<List<String>> blockingQueue = new LinkedBlockingQueue<>();

    @BeforeEach
    void init() {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @Test
    @DisplayName("테스트 주문 데이터 삽입")
    void registerTestOrder() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10000; i++) {
            OrderDto orderDto = OrderDto.of(
                    "buy",
                    "btc",
                    priceList.get(random.nextInt(0, priceList.size())),
                    random.nextLong(0, 10000),
                    random.nextLong(0, 10000),
                    random.nextDouble(0, 10),
                    System.currentTimeMillis()
            );

            tradeService.registerOrder(orderDto);
        }
    }

    /**
     * TODO : VisualVM 으로 관찰
     */
    @Test
    @DisplayName("RedissonOrderReaderAsyncRecursionLoop - CompletableFuture loop - test")
    void asyncLoopTest() throws InterruptedException {
        // given
        String keyName = "order:buy:btc:";
        BatchOptions batchOptions = BatchOptions.defaults();

        // when
        IntStream.range(0, THREAD_POOL_SIZE).forEach(value -> asyncLoop(keyName, batchOptions, getPrice()));
        Thread.sleep(120000);
    }

    /**
     * 테스트 데이터 timestamp 범위
     * "1703864310796"
     * "1703864364332"
     */
    private void asyncLoop(String keyName, BatchOptions batchOptions, Double price) {
        CompletableFuture<List<String>> cf = CompletableFuture.supplyAsync(
                getRedissonReaderSupplier(keyName, batchOptions, price), threadPool
        );

        cf
                .thenAccept(result -> {
                    // result input
                    if(Objects.nonNull(result)) {
                        blockingQueue.add(result);
                        asyncLoop(keyName, batchOptions, getPrice());
                    }
                })
                .exceptionally(throwable -> {
                    // retry logic, n번 시도한다와 같이 구현
                    logger.error(throwable.getMessage());
                    asyncLoop(keyName, batchOptions, price);

                    return null;
                });
    }

    private Supplier<List<String>> getRedissonReaderSupplier(String keyName, BatchOptions batchOptions, Double price) {
        return () -> {
            RLock lock = redissonClient.getLock(keyName + price + ":" + "lock");
            boolean lockResult = false;
            List<String> members = null;
            // read & lock


            if (lock.tryLock()) {

                RBatch batch = redissonClient.createBatch();

                List<CompletableFuture<String>> completableFutures = IntStream.range(0, 2)
                        .mapToObj(i -> i)
                        .flatMap(i -> Stream.of(
                                batch.getScoredSortedSet(keyName + price)
                                        .valueRangeAsync(1703864310796., true, ThreadLocalRandom.current().nextDouble(1703864310796., 1703864364332.), true)
                                        .thenApply(res -> "sell:" + res)
                                        .toCompletableFuture(),
                                batch.getScoredSortedSet(keyName + price)
                                        .valueRangeAsync(1703864310796., true, ThreadLocalRandom.current().nextDouble(1703864310796., 1703864364332.), true)
                                        .thenApply(res -> "buy:" + res)
                                        .toCompletableFuture()
                        ))
                        .toList();

                try {
                    batch.execute();
                    List<String> strings = completableFutures.stream()
                            .map(CompletableFuture::join)
                            .toList();
                } catch (RuntimeException e) {
                    throw new RuntimeException(e);
                }
            }

            return null;
        };
    }

    private Double getPrice() {
        return priceList.get(ThreadLocalRandom.current().nextInt(0, priceList.size()));
    }
}