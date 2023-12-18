package org.coin.price.listener;

import lombok.RequiredArgsConstructor;
import org.coin.price.dto.CryptoCoin;
import org.coin.price.dto.CurrentPrice;
import org.coin.price.event.PriceMessageProduceEvent;
import org.coin.price.queue.MessageQueue;
import org.coin.price.queue.PriceMessageWindowBlockingQueue;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceMessageProduceEventListener {
    private final PriceMessageWindowBlockingQueue messageQueue;
    private final CurrentPrice currentPrice;

    @EventListener
    public void handleEvent(PriceMessageProduceEvent event) {
        messageQueue.produce(event);
        currentPrice.setCurrentPrice(event);
    }
}
