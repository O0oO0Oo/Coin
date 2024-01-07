package org.coin.trade.pipeline.eventloop.read;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.trade.dto.pipeline.async.reader.OrderSortedSetDto;
import org.coin.trade.dto.pipeline.event.reader.ReadOrderDto;
import org.coin.trade.pipeline.eventloop.common.Script;
import org.coin.trade.pipeline.eventloop.write.WriterEvent;
import org.coin.trade.pipeline.eventloop.write.WriterEventQueue;
import org.coin.trade.redis.CustomOrderLock;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * TODO : Reader 랑 기능 분리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadRedissonOrderScript implements Script<List<CryptoCoin>> {
    private final RedissonClient redissonClient;
    private final ReaderEventQueue readerEventQueue;
    private final WriterEventQueue writerEventQueue;

    private final BatchOptions batchOptions = BatchOptions.defaults();
    private final String BUY_ORDER_KEY_PREFIX = "order:buy:";
    private final String SELL_ORDER_KEY_PREFIX = "order:sell:";

    @Override
    public void run(List<CryptoCoin> prices, Consumer<List<CryptoCoin>> onSuccess, BiConsumer<Throwable, List<CryptoCoin>> onFailure) {
        try{
            if (prices.isEmpty()) {
                return;
            }

            // Redisson 데이터 읽기
            ReadOrderDto readOrderDto = readOrders(prices);

            // TODO : 블로킹 후 락을 해제할 것인지 아니면? 아래에 비동기 코드 추가,

            // 쓰기 이벤트 발행
            writerEventQueue.add(new WriterEvent(readOrderDto));

            onSuccess.accept(prices);
        } catch (RuntimeException e) {
            // 실패시 다시 이벤트 발행.
            readerEventQueue.add(new ReadRedissonOrderEvent(prices));
            onFailure.accept(e, prices);
        }
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

            return new ReadOrderDto(lock, orderFutures);
        } catch (RedisException e) {
            log.error("Batch operation for {} discarded.", lock.getName(), e);
            batch.discard();
            return null;
        }
    }

    // TODO : 이전에는 비어있다면, 결과를 미리 얻어 락을 풀었지만, 비동기 작업 완료후 풀도록 작성해야한다.
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
}
