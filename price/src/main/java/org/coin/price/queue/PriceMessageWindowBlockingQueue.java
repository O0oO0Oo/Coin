package org.coin.price.queue;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.price.dto.CryptoCoinComparator;
import org.coin.price.dto.PriceApiRequest;
import org.coin.price.event.PriceMessageProduceEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PriceMessageWindowBlockingQueue implements MessageQueue<PriceMessageProduceEvent, List<CryptoCoin>> {
    private ConcurrentHashMap<String, PriorityBlockingQueue<CryptoCoin>> priceHashMapPriorityQueue = new ConcurrentHashMap<>();
    private ArrayList<String> coins = new ArrayList<>();
    private final AtomicInteger coinsIndex = new AtomicInteger(0);
    @Value("${price.api.initial-queue-size}")
    private int queueSize;
    @Value("${price.api.price-window-size}")
    private int windowSize;
    private final Map<String, ReentrantLock> reentrantLockMap = new HashMap<>();

    @PostConstruct
    void init() {
        log.debug("PriceMessageBlockingQueue init.");

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("BaseCryptoList.txt")) {
            coins = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            log.error("PriceMessageBlockingQueue PostConstruct Failed. : {}", e.getMessage());
        }

        coins.forEach(coinName -> {
            priceHashMapPriorityQueue.put(coinName, new PriorityBlockingQueue<>(queueSize, new CryptoCoinComparator()));
            reentrantLockMap.put(coinName, new ReentrantLock());
        });
    }

    @Override
    public void produce(PriceMessageProduceEvent event) {
        Long timestamp = event.timestamp();
        Map<String, PriceApiRequest.PriceData> priceDataMap = event.priceDataMap();

        priceDataMap.forEach((key, value) -> {
            CryptoCoin coin = buildCryptoCoin(key, value, timestamp);
            addPricePriorityBlockingQueue(key, coin);
        });
    }

    private CryptoCoin buildCryptoCoin(String key, PriceApiRequest.PriceData value, Long timestamp) {
        return CryptoCoin.builder()
                .price(value.getClosing_price())
                .coinName(key)
                .timestamp(timestamp)
                .build();
    }

    private void addPricePriorityBlockingQueue(String key, CryptoCoin coin) {
        this.priceHashMapPriorityQueue.computeIfPresent(key, (k, blockingQueue) -> {
            blockingQueue.put(coin);
            return blockingQueue;
        });
    }

    @Override
    public List<CryptoCoin> consume() {
        return tumblingWindow(
                getCoinName()
        );
    }

    private String getCoinName() {
        return coins.get(getCoinsIndex());
    }

    private int getCoinsIndex() {
        return coinsIndex.getAndAccumulate(
                1,
                (current, update) -> {
                    if (current < coins.size() - 1) {
                        return current + update;
                    }
                    return 0;
                });
    }

    private List<CryptoCoin> tumblingWindow(String name) {
        if (reentrantLockMap.get(name).tryLock()) {
            try {
                PriorityBlockingQueue<CryptoCoin> coinBlockingQueue = priceHashMapPriorityQueue.get(name);
                Map<String, CryptoCoin> windowMap = new HashMap<>(windowSize + 1, 1.0f);

                while (windowMap.keySet().size() < windowSize && coinBlockingQueue.peek() != null) {
                    CryptoCoin coin = coinBlockingQueue.poll();
                    windowMap.put(coin.getPrice(), coin);
                }
                return windowMap.values().stream().toList();
            } finally {
                reentrantLockMap.get(name).unlock();
            }
        } else {
            return Collections.emptyList();
        }
    }
}