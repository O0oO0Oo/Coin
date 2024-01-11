package org.coin.trade.dto.pipeline.event.lock;

/**
 * [lock:order:BTC:5000.0:1704910701700:1704910702700]
 * 락의 결과 dto
 * coin name
 * price
 * timestamp begin
 * timestamp end
 */
public record LockResultDto(
        String name,
        double price,
        double beginTimestamp,
        double endTimestamp
) {
    public static LockResultDto of(String lockResult) {
        String[] result = lockResult.split(":");
        return new LockResultDto(result[2], Double.parseDouble(result[3]), Double.parseDouble(result[4]), Double.parseDouble(result[5]));
    }

    public String buyOrderKey() {
        return "order:buy:" + name + ":" + price;
    }

    public String sellOrderKey() {
        return "order:sell:" + name + ":" + price;
    }

    public String lockKey() {
        return "lock:order:" + name + ":" + price;
    }
}
