package org.coin.trade.dto.pipeline.writer;

import java.util.List;

/**
 * @param type buy/sell
 * @param orderIds 처리된 주문 아이디 목록
 * @param processedOrderDtoList
 */
public record WriteOrderDto(
        String type,
        List<Long> orderIds,
        List<ProcessedOrderDto> processedOrderDtoList
) {
    public static WriteOrderDto of(String type, List<Long> orderIds, List<ProcessedOrderDto> processedOrderDtoList) {
        return new WriteOrderDto(type, orderIds, processedOrderDtoList);
    }
}
