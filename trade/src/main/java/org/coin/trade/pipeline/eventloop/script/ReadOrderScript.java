package org.coin.trade.pipeline.eventloop.script;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.trade.dto.pipeline.event.reader.OrderSortedSetDto;
import org.coin.trade.dto.pipeline.event.reader.ReadOrderDto;
import org.coin.trade.pipeline.eventloop.event.ReadOrderEvent;
import org.coin.trade.pipeline.eventloop.event.WriteOrderEvent;
import org.coin.trade.pipeline.eventloop.redis.OrderLock;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 레디스에서 락을 걸고 읽기위한 스크립트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadOrderScript implements Script<List<CryptoCoin>> {
    private final RedissonClient redissonClient;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchOptions batchOptions = BatchOptions.defaults();

    @Override
    public void run(List<CryptoCoin> prices, Consumer<List<CryptoCoin>> onSuccess, BiConsumer<Throwable, List<CryptoCoin>> onFailure) {
        if (prices.isEmpty()) {
            return;
        }

        // 락 시도
        OrderLock lock = new OrderLock(redissonClient, prices, 6000);
        lock.tryLockAsync()
                .thenAccept(lockResult -> {
                    // 획득한 락이 없다면 실행 x
                    if (lockResult.isEmpty()) {
                        return;
                    }
                    // 획득한 락 종류에 대해 읽기 수행
                    executeBatchOperation(lock);
                    // 성공
                    onSuccess.accept(prices);
                })
                .exceptionally(throwable -> {
                    // 실패, 읽기 다시시도
                    eventPublisher.publishEvent(new ReadOrderEvent(prices));
                    onFailure.accept(throwable, prices);
                    return null;
                });
    }

    private void executeBatchOperation(OrderLock lock) {
        RBatch batch = redissonClient.createBatch(batchOptions);
        createWriteEvent(lock, batch);
        batch.executeAsync();
    }

    private void createWriteEvent(OrderLock lock, RBatch batch) {
        List<CompletableFuture<Optional<?>>> completableFutures = lock.getLockResultDtoList()
                .stream().flatMap(lockResultDto ->
                        Stream.of(
                                /**
                                 * 다음과 같이 주문들을 검색하고 없다면 Optional 을 반환하여 후에 CompletableFuture 에서 처리한다.
                                 * ZRANGEBYSCORE 로 10시0분0초에 BTC는 5000원 이였다 이를 기반으로 유저가 등록한 주문 중 10시0분0초 까지 등록한 주문을 검색한다.
                                 * ZRANGEBYSCORE key = ..BTC:4000 score 0 ~ 10시0분0초
                                 * 또는
                                 * 다른 스레드에서 락을 획득했다면
                                 * 스레드 1번 ZRANGEBYSCORE key = ..BTC:4000 score 0 ~ 10시0분0초
                                 * 스레드 2번 ZRANGEBYSCORE key = ..BTC:4000 score 10시0분0초 ~ 10시0분10초
                                 * 다음과 같이 검색하여 주문이 중복처리 되지 않도록 한다.
                                 */
                                // buy orders
                                batch.getScoredSortedSet(lockResultDto.buyOrderKey())
                                        .valueRangeAsync(lockResultDto.beginTimestamp(), false, lockResultDto.endTimestamp(), true)
                                        .thenApply(res -> res.isEmpty() ? Optional.empty() : Optional.of(OrderSortedSetDto.of("buy", lockResultDto, res)))
                                        .toCompletableFuture(),
                                // sell orders
                                batch.getScoredSortedSet(lockResultDto.sellOrderKey())
                                        .valueRangeAsync(lockResultDto.beginTimestamp(), false, lockResultDto.endTimestamp(), true)
                                        .thenApply(res -> res.isEmpty() ? Optional.empty() : Optional.of(OrderSortedSetDto.of("sell", lockResultDto, res)))
                                        .toCompletableFuture()
                        )
                ).toList();

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> completableFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList())
                .thenAccept(results -> {
                    // 데이터가 있다면 쓰기위해 이벤트 발행.
                    if (!results.isEmpty()) {
                        eventPublisher.publishEvent(new WriteOrderEvent(ReadOrderDto.of(lock, (List<OrderSortedSetDto>) results)));
                    }
                    else {
                        // 읽기 후, 읽은 데이터가 없다면 바로 unlock
                        lock.unlockAsync();
                    }
                });
    }
}