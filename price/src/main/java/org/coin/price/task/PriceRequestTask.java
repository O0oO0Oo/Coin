package org.coin.price.task;

import lombok.RequiredArgsConstructor;
import org.coin.price.dto.PriceApiRequest;
import org.coin.price.event.AsyncSchedulingFailureCountEvent;
import org.coin.price.event.PriceMessageProduceEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PriceRequestTask {
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${price.api.url}")
    private String API_URL;

    @Async("priceRequestTaskExecutor")
    @Scheduled(fixedDelay = 250L, initialDelay = 1000L)
    public void requestScheduler() {
        PriceApiRequest priceApiRequest = apiRequest();

        eventPublisher.publishEvent(PriceMessageProduceEvent.of(priceApiRequest));
        eventPublisher.publishEvent(AsyncSchedulingFailureCountEvent.success());
    }

    private PriceApiRequest apiRequest() {
        return restTemplate.getForObject(API_URL, PriceApiRequest.class);
    }
}