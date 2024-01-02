package org.coin.trade.dto.service;

/**
 * Key = order:{type}:{coinName}:{price}
 * @param type
 * @param coinName
 * @param price
 *
 * Member = {orderId}:{walletId}:{userId}:{quantity}
 * @param orderId
 * @param walletId
 * @param userId
 * @param quantity
 */
public record RegisterOrderDto(
        String type,
        String coinName,
        String price,
        long orderId,
        long walletId,
        long userId,
        double quantity,
        double timestamp
        ) {

        public static RegisterOrderDto of(String type, String coinName, double price, long orderId, long walletId, long userId, double quantity) {
                return new RegisterOrderDto(
                        type,
                        coinName,
                        String.valueOf(price),
                        orderId,
                        walletId,
                        userId,
                        quantity,
                        System.currentTimeMillis()
                );
        }

        public String key() {
                return "order:" + type + ":" + coinName + ":" + price;
        }

        public String member() {
                return orderId + ":" + walletId + ":" + userId + ":" + quantity;
        }
}
