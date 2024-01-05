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
public record OrderDto(
        String type,
        String coinName,
        String price,
        long orderId,
        long walletId,
        long userId,
        double quantity,
        long timestamp
        ) {

        public static OrderDto of(String type, String coinName, double price, long orderId, long walletId, long userId, double quantity, long timestamp) {
                return new OrderDto(
                        type,
                        coinName,
                        String.valueOf(price),
                        orderId,
                        walletId,
                        userId,
                        quantity,
                        timestamp
                );
        }

        public String key() {
                return "order:" + type + ":" + coinName + ":" + price;
        }

        public String member() {
                return orderId + ":" + walletId + ":" + userId + ":" + quantity;
        }

        public String history() {
                return "lock:history:" + coinName + ":" + price;
        }
}
