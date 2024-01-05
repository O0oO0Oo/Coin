package org.coin.trade.dto.service;

/**
 * Key = order:{type}:{coinName}:{price}
 * @param type
 * @param coinName
 * @param price
 *
 * Member = {orderId}:{processedId}:{quantity}
 * @param orderId
 * @param mainId type 이 buy 면 walletId, sell 이면 userId 를 입력.
 * @param quantity
 */
public record OrderDto(
        String type,
        String coinName,
        String price,
        long orderId,
        long mainId,
        double quantity,
        long timestamp
        ) {

        public static OrderDto of(String type, String coinName, double price, long orderId, long mainId, double quantity, long timestamp) {
                return new OrderDto(
                        type,
                        coinName,
                        String.valueOf(price),
                        orderId,
                        mainId,
                        quantity,
                        timestamp
                );
        }

        public String key() {
                return "order:" + type + ":" + coinName + ":" + price;
        }

        public String member() {
                return orderId + ":" + mainId + ":" + quantity;
        }

        public String history() {
                return "lock:history:" + coinName + ":" + price;
        }
}
