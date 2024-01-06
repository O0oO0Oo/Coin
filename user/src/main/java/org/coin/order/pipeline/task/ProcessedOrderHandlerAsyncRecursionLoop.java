package org.coin.order.pipeline.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coin.order.repository.OrderBulkUpdateRepository;
import org.coin.trade.dto.pipeline.writer.ProcessedOrderDto;
import org.coin.trade.dto.pipeline.writer.WriteOrderDto;
import org.coin.trade.pipeline.loop.AbstractAsyncRecursionLoop;
import org.coin.user.repository.UserBulkUpdateRepository;
import org.coin.wallet.repository.WalletBulkUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedOrderHandlerAsyncRecursionLoop extends AbstractAsyncRecursionLoop<WriteOrderDto, Boolean> {
    private OrderBulkUpdateRepository orderBulkUpdateRepository;
    private WalletBulkUpdateRepository walletBulkUpdateRepository;
    private UserBulkUpdateRepository userBulkUpdateRepository;
    private ProcessedOrderHandlerAsyncRecursionLoop self;

    @Autowired
    public ProcessedOrderHandlerAsyncRecursionLoop(OrderBulkUpdateRepository orderBulkUpdateRepository,
                                                   @Lazy ProcessedOrderHandlerAsyncRecursionLoop self,
                                                   @Qualifier("handlerThreadPool") Pair<ExecutorService, ExecutorService> threadPool,
                                                   ProcessedOrderHandler processedOrderHandler,
                                                   WalletBulkUpdateRepository walletBulkUpdateRepository,
                                                   UserBulkUpdateRepository userBulkUpdateRepository) {
        this.orderBulkUpdateRepository = orderBulkUpdateRepository;
        this.self = self;
        this.setMainThreadPool(threadPool.getFirst());
        this.setSwapThreadPool(threadPool.getSecond());
        this.setLoopSupplier(processedOrderHandler);
        this.walletBulkUpdateRepository = walletBulkUpdateRepository;
        this.userBulkUpdateRepository = userBulkUpdateRepository;
    }

    @Override
    protected Boolean processResult(WriteOrderDto writeOrderDto) {
        if (Objects.isNull(writeOrderDto)) {
            return null;
        }

        if(writeOrderDto.processedOrderDtoList().isEmpty()){
            return null;
        }

        if (writeOrderDto.type().equals("buy")) {
            self.saveBuyProcessedOrders(writeOrderDto);
        }

        if (writeOrderDto.type().equals("sell")) {
            self.saveSellProcessedOrders(writeOrderDto);
        }

        return null;
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


    @Override
    protected CompletableFuture<Void> doConcurrencyLevelControl(Boolean result) {
        // not used
        return null;
    }

    @Override
    protected Void doHandlerError(Throwable throwable) {
        log.error("An error occurred in the user pipeline - handler.", throwable);
        return null;
    }

    @Override
    protected boolean shouldStopAsyncLoop() {
        return false;
    }
}
