package org.coin.trade.pipeline.asyncloop.writer;

import lombok.RequiredArgsConstructor;
import org.coin.price.queue.MessageQueue;
import org.coin.trade.dto.pipeline.async.reader.OrderSortedSetDto;
import org.coin.trade.dto.pipeline.async.reader.ReadOrderDto;
import org.coin.trade.dto.pipeline.async.writer.ProcessedOrderDto;
import org.coin.trade.dto.pipeline.async.writer.WriteOrderDto;
import org.coin.trade.pipeline.asyncloop.queue.ProcessedOrderMessageBlockingQueue;
import org.coin.trade.pipeline.asyncloop.redis.CustomOrderLock;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

//@Component
@RequiredArgsConstructor
public class ProcessedOrderWriter implements ItemWriter<ReadOrderDto>{
    private final MessageQueue<ReadOrderDto, ReadOrderDto> messageQueue;
    private final RedissonClient redissonClient;
    private final ProcessedOrderMessageBlockingQueue processedOrderMessageBlockingQueue;

    private final BatchOptions batchOptions = BatchOptions.defaults();

    public Supplier<CustomOrderLock> writeSupplier() {
        return () -> {
            ReadOrderDto consume;

            try {
                consume = messageQueue.consume();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (Objects.isNull(consume)) {
                return null;
            }

            // 메시지 큐에 쓰기, 삭제
            write(consume);

            // 쓰기 성공 후 lock 해제
            return consume.lock();
        };
    }

    @Override
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
         * member = {orderId}:{walletId}:{userId}:{quantity}
         * score = timestamp
         * <p>
         * ex : order:buy:btc:100000 : buyOrderId:walletId:userId:quantity
         *      order:sell:btc:100000 : sellOrderId:walletId:userId:quantity
         */
        // buy 유저의 코인 수량 증가
        WriteOrderDto writerBuyOrderDto = processBuyOrder(pairOrderSortedSetDtoList.getFirst());
        processedOrderMessageBlockingQueue.produce(writerBuyOrderDto);

        // sell 유저의 잔액 증가
        WriteOrderDto writerSellOrderDto = processSellOrder(pairOrderSortedSetDtoList.getSecond());
        processedOrderMessageBlockingQueue.produce(writerSellOrderDto);

        // 삭제 배치처리
        executeBatchOperation(readOrderDto.orderSortedSetDtoList());
    }

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

    private WriteOrderDto processSellOrder(List<OrderSortedSetDto> sellOrderSortedSetDtoList) {
        Map<Long, ProcessedOrderDto> sellOrderMap = new HashMap<>();
        List<Long> sellOrderIdList = new LinkedList<>();

        sellOrderSortedSetDtoList
                .forEach(orderSortedSetDto -> {
                    double price = orderSortedSetDto.price();

                    orderSortedSetDto.orders()
                            .forEach(order -> {
                                String[] split = order.split(":");
                                long orderId = Long.parseLong(split[0]);
                                long userId = Long.parseLong(split[1]);
                                double quantity = Double.parseDouble(split[2]);

                                double money = quantity * price;

                                sellOrderIdList.add(orderId);
                                sellOrderMap.compute(
                                        userId,
                                        (k, v) -> (v == null) ? ProcessedOrderDto.of(userId, money) : v.increaseAmount(money)
                                );
                            });
                });
        return WriteOrderDto.of("sell", sellOrderIdList, sellOrderMap.values().stream().toList());
    }

    /**
     * 처리된 주문 삭제
     * TODO : member 삭제 현재는 ZERM 으로 m*log(n) 이 걸려서 ZREMRANGEBYSCORE M + lon(n) 보다 느릴것으로 예상
     *        하지만 실제로 내부에서 얼마나 차이가 있는지 모르겠다.
     */
    private void executeBatchOperation(List<OrderSortedSetDto> orderSortedSetDtoList) {
        RBatch batch = redissonClient.createBatch(batchOptions);

        orderSortedSetDtoList
                .forEach(dto ->
                        batch.getScoredSortedSet(dto.key())
                                .removeAllAsync(dto.orders())
                );

        try{
            batch.execute();
        } catch (RedisException e) {
            batch.discard();
            throw new RedisException(e);
        }
    }
}
