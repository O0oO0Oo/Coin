package org.coin.trade.pipeline.eventloop.lua;

import org.coin.price.dto.CryptoCoin;
import org.coin.trade.config.RedissonWriteToMasterReadFromReplicaConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@ActiveProfiles("test")
@SpringBootTest(classes = {RedissonWriteToMasterReadFromReplicaConfiguration.class})
public class TradeLuaTest {
    @Autowired
    private RedissonClient redissonClient;

    @Test
    @DisplayName("lua script for EventLoop - lockWriteHistory")
    void should_lockAndWriteHistory_when_readPrice() {
        // given
        long timestamp = System.currentTimeMillis();
        CryptoCoin coin = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(timestamp - 1000)
                .price(5000).build();
        CryptoCoin coin1 = CryptoCoin.builder()
                .coinName("XRP")
                .timestamp(timestamp - 750)
                .price(3000).build();
        CryptoCoin coin2 = CryptoCoin.builder()
                .coinName("ETH")
                .timestamp(timestamp - 750)
                .price(5000).build();
        CryptoCoin coin3 = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(timestamp)
                .price(5000).build();
        List<CryptoCoin> coins = List.of(coin, coin1, coin2);

        // fail - 현재 처리중인 가격 10:00:01 시간일때 BTC 5000.0 원,
        // 아래는 10:00:00.750 시간일때 BTC 5000.0 원 가격 즉 처리되면 안되기에 0개를 리턴해야한다.
        CryptoCoin coin4 = CryptoCoin.builder()
                .coinName("BTC")
                .timestamp(timestamp - 250)
                .price(5000).build();

        // when
        List<String> result1 = lockWriteHistory(coins, 20000);
        List<String> result2 = lockWriteHistory(List.of(coin3), 20000);
        List<String> result3 = lockWriteHistory(List.of(coin4), 20000);

        RMap<String, String> map = redissonClient.getMap("history:lock:order:BTC:5000.0");
        String resultTS = map.get("timestamp");

        // then
        Assertions.assertAll(
                () -> Assertions.assertEquals(3, result1.size(), "Size should be 3"),
                () -> Assertions.assertEquals(1, result2.size(), "Size should be 1"),
                () -> Assertions.assertEquals(0, result3.size(), "Size should be 0."),
                () -> Assertions.assertEquals(timestamp, Long.valueOf(resultTS), "timestamp should be " + timestamp)
        );
    }

    private List<String> lockWriteHistory(List<CryptoCoin> prices, int leaseTime) {
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        values.add(String.valueOf(Thread.currentThread().getId()));
        values.add(leaseTime);

        prices.forEach(price -> {
            String key = "lock:order:" + price.getCoinName() + ":" + price.getPrice();
            keys.add(key);
            values.add(String.valueOf(price.getTimestamp()));
        });

        String lock = getLuaAsString("lua/lockWriteHistory.lua");
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.eval(RScript.Mode.READ_WRITE, lock, RScript.ReturnType.MULTI, keys, values.toArray(new Object[0]));
    }
    private String getLuaAsString(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            InputStream inputStream = resource.getInputStream();
            Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}