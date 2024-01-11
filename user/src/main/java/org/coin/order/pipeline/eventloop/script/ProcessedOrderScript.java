package org.coin.order.pipeline.eventloop.script;

import org.coin.order.repository.OrderBulkUpdateRepository;
import org.coin.trade.dto.pipeline.async.writer.ProcessedOrderDto;
import org.coin.trade.dto.pipeline.async.writer.WriteOrderDto;
import org.coin.trade.pipeline.eventloop.script.Script;
import org.coin.user.repository.UserBulkUpdateRepository;
import org.coin.wallet.repository.WalletBulkUpdateRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 전처리된 주문을 저장하기위한 스크립트
 */
@Component
public class ProcessedOrderScript implements Script<WriteOrderDto> {
    private final OrderBulkUpdateRepository orderBulkUpdateRepository;
    private final WalletBulkUpdateRepository walletBulkUpdateRepository;
    private final UserBulkUpdateRepository userBulkUpdateRepository;
    private final ProcessedOrderScript self;

    public ProcessedOrderScript(OrderBulkUpdateRepository orderBulkUpdateRepository,
                                WalletBulkUpdateRepository walletBulkUpdateRepository,
                                UserBulkUpdateRepository userBulkUpdateRepository,
                                @Lazy ProcessedOrderScript self) {
        this.orderBulkUpdateRepository = orderBulkUpdateRepository;
        this.walletBulkUpdateRepository = walletBulkUpdateRepository;
        this.userBulkUpdateRepository = userBulkUpdateRepository;
        this.self = self;
    }

    @Override
    public void run(WriteOrderDto input, Consumer<WriteOrderDto> onSuccess, BiConsumer<Throwable, WriteOrderDto> onFailure) {
        if (input.type().equals("buy")) {
            self.saveBuyProcessedOrders(input);
        }

        if (input.type().equals("sell")) {
            self.saveSellProcessedOrders(input);
        }
    }

    @Transactional
    protected void saveBuyProcessedOrders(WriteOrderDto writeOrderDto) {
        List<Long> orderIds = writeOrderDto.orderIds();
        List<ProcessedOrderDto> processedOrderDtoList = writeOrderDto.processedOrderDtoList();

        // orderId
        orderBulkUpdateRepository.buyOrderBulkUpdateOperation(orderIds);

        // coin quantity
        walletBulkUpdateRepository.walletBulkUpdateOperation(processedOrderDtoList);
    }

    @Transactional
    protected void saveSellProcessedOrders(WriteOrderDto writeOrderDto) {
        List<Long> orderIds = writeOrderDto.orderIds();
        List<ProcessedOrderDto> processedOrderDtoList = writeOrderDto.processedOrderDtoList();

        // sell orderId
        orderBulkUpdateRepository.sellOrderBulkUpdateOperation(orderIds);

        // user money
        userBulkUpdateRepository.userBulkUpdateOperation(processedOrderDtoList);
    }
}