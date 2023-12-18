package org.coin.trade.task;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.coin.price.dto.CryptoCoin;
import org.coin.price.event.PriceMessageProduceEvent;
import org.coin.price.queue.MessageQueue;
import org.coin.price.queue.PriceMessageWindowBlockingQueue;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ProcessOrderTask implements Runnable {
    private final PriceMessageWindowBlockingQueue messageQueue;
    private final RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        runTask();
    }

    private void runTask() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        executorService.submit(this);
        executorService.submit(this);
        executorService.submit(this);
        executorService.submit(this);
    }

    // TODO : Async Scheduling 을 할지 아니면 다른 구조를 사용할지 결정
    @Override
    public void run() {
        while (true) {
            List<CryptoCoin> coins = messageQueue.consume();
            if (!coins.isEmpty()) {
                coins.forEach(coin ->
                        redisTemplate.opsForZSet().range("test", 0, 1000).stream().toList()
                );

                // Do Something
                // ...
            }
        }
    }
}
