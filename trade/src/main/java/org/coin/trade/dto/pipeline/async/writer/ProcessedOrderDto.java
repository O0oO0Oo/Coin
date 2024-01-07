package org.coin.trade.dto.pipeline.async.writer;

import lombok.Getter;

/**
 * id = buy : walletId, sell : userId
 * amount = buy : crypto quantity, sell : amount of user money
 */
@Getter
public class ProcessedOrderDto {
    private long id;
    private double amount;

    public ProcessedOrderDto(long id, double amount) {
        this.id = id;
        this.amount = amount;
    }

    public static ProcessedOrderDto of(long id, double amount) {
        return new ProcessedOrderDto(id, amount);
    }

    public ProcessedOrderDto increaseAmount(double amount){
        this.amount += amount;
        return this;
    }
}
