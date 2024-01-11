package org.coin.trade.pipeline.eventloop.script;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.trade.dto.pipeline.event.reader.OrderSortedSetDto;
import org.coin.trade.dto.pipeline.async.writer.ProcessedOrderDto;
import org.coin.trade.dto.pipeline.async.writer.WriteOrderDto;
import org.coin.trade.dto.pipeline.event.reader.ReadOrderDto;
import org.coin.trade.pipeline.eventloop.event.ProcessedOrderEvent;
import org.coin.trade.pipeline.eventloop.event.WriteOrderEvent;
import org.redisson.api.*;
import org.redisson.client.RedisException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 읽은 데이터 db에 넣기전 전처리 스크립트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WriteOrderScript implements Script<ReadOrderDto> {
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final BatchOptions batchOptions = BatchOptions.defaults();

    @Override
    public void run(ReadOrderDto input, Consumer<ReadOrderDto> onSuccess, BiConsumer<Throwable, ReadOrderDto> onFailure) {
        executeAsyncBatchOperation(input.orderSortedSetDtoList())
                .thenAccept(x -> {
                    // 쓰기위해 데이터 전처리
                    write(input);

                    // 성공하면 락 해제
                    input.lock().unlockAsync();
                    onSuccess.accept(input);
                })
                .exceptionally(throwable -> {
                    // 실패 -> 이벤트 재발행
                    eventPublisher.publishEvent(new WriteOrderEvent(input));
                    onFailure.accept(throwable, input);
                    return null;
                });

    }

    public void write(ReadOrderDto readOrderDto) {
        // TODO : 처리된 거래가 없을시 진행 x
        // Pair<buy, sell>
        Pair<List<OrderSortedSetDto>, List<OrderSortedSetDto>> pairOrderSortedSetDtoList = getPairOrderSortedSetDtoList(readOrderDto);

        // 1. Map id, dto 에 저장한다 - buy, sell 모두
        // 2. orderId 리스트 저장

        /**
         * 주문 등록
         * SortedSet
         * key = order:{type}:{coin name}:{price}
         * member = {orderId}:{userId/walletId}:{quantity}
         * score = timestamp
         * <p>
         * ex : order:buy:btc:100000 : buyOrderId:walletId:quantity
         *      order:sell:btc:100000 : sellOrderId:userId:quantity
         */
        // 삭제 배치처리

        // buy 유저의 코인 수량 증가, 값이 있을때
        WriteOrderDto writerBuyOrderDto = processBuyOrder(pairOrderSortedSetDtoList.getFirst());
        if(!writerBuyOrderDto.orderIds().isEmpty()) {
            eventPublisher.publishEvent(new ProcessedOrderEvent(writerBuyOrderDto));
        }
        // sell 유저의 잔액 증가, 값이 있을떄
        WriteOrderDto writerSellOrderDto = processSellOrder(pairOrderSortedSetDtoList.getSecond());
        if(!writerSellOrderDto.orderIds().isEmpty()) {
            eventPublisher.publishEvent(new ProcessedOrderEvent(writerSellOrderDto));
        }
    }

    /**
     * 전처리 후 취합
     */
    private Pair<List<OrderSortedSetDto>, List<OrderSortedSetDto>> getPairOrderSortedSetDtoList(ReadOrderDto readOrderDto) {
        List<OrderSortedSetDto> orderSortedSetDtoList = readOrderDto.orderSortedSetDtoList();

        List<OrderSortedSetDto> buyOrderSortedSetDtoList = new ArrayList<>();
        List<OrderSortedSetDto> sellOrderSortedSetDtoList = new ArrayList<>();

        orderSortedSetDtoList
                .forEach(dto -> {
                    if (dto.type().equals("buy")) {
                        buyOrderSortedSetDtoList.add(dto);
                    }
                    if (dto.type().equals("sell")) {
                        sellOrderSortedSetDtoList.add(dto);
                    }
                });
        return Pair.of(buyOrderSortedSetDtoList, sellOrderSortedSetDtoList);
    }

    /**
     * 구매 주문 전처리
     */
    private WriteOrderDto processBuyOrder(List<OrderSortedSetDto> buyOrderSortedSetDtoList) {
        Map<Long, ProcessedOrderDto> buyOrderMap = new HashMap<>();
        List<Long> buyOrderIdList = new LinkedList<>();

        buyOrderSortedSetDtoList
                .forEach(orderSortedSetDto -> {
                    orderSortedSetDto.orders()
                            .forEach(order -> {
                                String[] split = order.split(":");
                                long orderId = Long.parseLong(split[0]);
                                long walletId = Long.parseLong(split[1]);
                                double quantity = Double.parseDouble(split[2]);

                                buyOrderIdList.add(orderId);
                                buyOrderMap.compute(
                                        walletId,
                                        (k, v) -> (v == null) ? ProcessedOrderDto.of(walletId, quantity) : v.increaseAmount(quantity)
                                );
                            });
                });
        return WriteOrderDto.of("buy", buyOrderIdList, buyOrderMap.values().stream().toList());
    }

    /**
     * 판매 주문 전처리
     */
    private WriteOrderDto processSellOrder(List<OrderSortedSetDto> sellOrderSortedSetDtoList) {
        Map<Long, ProcessedOrderDto> sellOrderMap = new HashMap<>();
        List<Long> sellOrderIdList = new LinkedList<>();

        sellOrderSortedSetDtoList
                .forEach(orderSortedSetDto -> orderSortedSetDto.orders()
                        .forEach(order -> {
                            String[] split = order.split(":");
                            long orderId = Long.parseLong(split[0]);
                            long userId = Long.parseLong(split[1]);
                            double quantity = Double.parseDouble(split[2]);

                            double money = quantity * orderSortedSetDto.price();

                            sellOrderIdList.add(orderId);
                            sellOrderMap.compute(
                                    userId,
                                    (k, v) -> (v == null) ? ProcessedOrderDto.of(userId, money) : v.increaseAmount(money)
                            );
                        }));
        return WriteOrderDto.of("sell", sellOrderIdList, sellOrderMap.values().stream().toList());
    }

    /**
     * 처리된 주문 삭제
     */
    private RFuture<BatchResult<?>> executeAsyncBatchOperation(List<OrderSortedSetDto> orderSortedSetDtoList) {
        RBatch batch = redissonClient.createBatch(batchOptions);

        orderSortedSetDtoList
                .forEach(dto ->
                        batch.getScoredSortedSet(dto.key())
                                .removeAllAsync(dto.orders())
                );

        // 삭제 실행.
        return batch.executeAsync();
    }
}