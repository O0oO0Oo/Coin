package org.coin.trade.pipeline.reader;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.price.queue.PriceMessageWindowBlockingQueue;
import org.coin.trade.dto.pipeline.reader.OrderSortedSetDto;
import org.coin.trade.dto.pipeline.reader.ReadOrderDto;
import org.coin.trade.redis.CustomOrderLock;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 1. 읽기 실패, 재시도 로직
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonOrderReader implements ItemReader<ReadOrderDto> {
    @Qualifier("readerFastThreadLocal")
    private final FastThreadLocal<List<CryptoCoin>> readerThreadLocal;
    private final RedissonClient redissonClient;
    private final PriceMessageWindowBlockingQueue priceQueue;

    private final BatchOptions batchOptions = BatchOptions.defaults();
    private final String BUY_ORDER_KEY_PREFIX = "order:buy:";
    private final String SELL_ORDER_KEY_PREFIX = "order:sell:";


    public Supplier<ReadOrderDto> getReadSupplier() {
        return this::read;
    }

    /**
     * 이전에 실패한 가격처리가 있다면 재시도 한다.
     */
    @Override
    public ReadOrderDto read() {
        List<CryptoCoin> prices;

        if(readerThreadLocal.isSet()) {
            // 이전에 실패한 경우 이전의 가격을 불러옴.
            prices = readerThreadLocal.get();
        }
        else{
            prices = priceQueue.consume();
            readerThreadLocal.set(prices);
        }

        if (prices.isEmpty()) {
            return null;
        }

        return readOrders(prices);
    }

    /**
     * lock 은 writer 에서 처리 후 해제
     */
    private ReadOrderDto readOrders(List<CryptoCoin> prices) {
        CustomOrderLock lock = new CustomOrderLock(redissonClient, prices);

        if (lock.tryLock(6000)) {
            log.info(lock.getName() + " is locked.");
            return executeBatchOperation(lock, prices);
        }

        return null;
    }

    private ReadOrderDto executeBatchOperation(CustomOrderLock lock, List<CryptoCoin> prices) {
        RBatch batch = redissonClient.createBatch(batchOptions);
        List<CompletableFuture<OrderSortedSetDto>> orderFutures = createOrderFutures(prices, batch);

        try {
            batch.execute();
            List<OrderSortedSetDto> orders = collectOrderResults(orderFutures);

            return new ReadOrderDto(lock, orders);
        } catch (RedisException e) {
            log.error("Batch operation for {} discarded.", lock.getName(), e);
            batch.discard();
            return null;
        }
    }

    private List<CompletableFuture<OrderSortedSetDto>> createOrderFutures(List<CryptoCoin> prices, RBatch batch) {
        return prices.stream()
                .flatMap(price -> Stream.of(
                        // buy orders
                        batch.getScoredSortedSet(SELL_ORDER_KEY_PREFIX + price.toString())
                                .valueRangeAsync(0, true, price.getTimestamp(), true)
                                .thenApply(res -> {
                                    if(!res.isEmpty()) {
                                        return OrderSortedSetDto.of("sell", price, res);
                                    }
                                    return null;
                                })
                                .toCompletableFuture(),
                        // sell orders
                        batch.getScoredSortedSet(BUY_ORDER_KEY_PREFIX + price.toString())
                                .valueRangeAsync(0, true, price.getTimestamp(), true)
                                .thenApply(res -> {
                                    if(!res.isEmpty()) {
                                        return OrderSortedSetDto.of("buy", price, res);
                                    }
                                    return null;
                                })
                                .toCompletableFuture()
                )).toList();
    }

    private List<OrderSortedSetDto> collectOrderResults(List<CompletableFuture<OrderSortedSetDto>> futures) {
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }
}