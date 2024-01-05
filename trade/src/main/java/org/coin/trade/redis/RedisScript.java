package org.coin.trade.redis;

public class RedisScript {
    public static final String LOCK_WRITE_HISTORY = "" + //
            // lock / thread-id, PEXPIRE
            "local lock = redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX') " +
            "if lock == false then " +
                // 락이 이미 있으면 false
                "return false " +
            "end " +
            // 락이 없으면 기록, true 리턴
            // key = lock:history:BTC:4000.0 / timestamp = 1702020200
            "for i = 2, #KEYS do " +
                "redis.call('HSET', KEYS[i], 'timestamp', ARGV[i+1]) " +
                "redis.call('PEXPIRE', KEYS[i], ARGV[2]) " +
            "end " +
            "return true ";
    public static final String UNLOCK_DELETE_HISTORY = "" +
            "for i = 1, #KEYS do " +
                "redis.call('DEL', KEYS[i]) " +
            "end " +
            "return true ";

    public static final String CHECK_LOCK_ZREM = "" +
            // 락 존재 확인
            "if redis.call('EXISTS', KEYS[1]) == 1 then " +
                // 기록 존재 확인
                "if redis.call('HGET', KEYS[2], 'timestamp') then " +
                    "if redis.call('HGET', KEYS[2], 'timestamp') <= ARGV[1] then " +
                        // ZREM 삭제
                        "redis.call('ZREM', KEYS[3], ARGV[2]) " +
                        "return true " +
                    "end " +
                "end " +
                "return false " +
            "end " +
            "redis.call('ZREM', KEYS[3], ARGV[2]) " +
            "return true ";
}