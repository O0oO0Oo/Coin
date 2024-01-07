package org.coin.trade.pipeline.eventloop.read;

import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.trade.pipeline.eventloop.common.AbstractEvent;
import org.coin.trade.pipeline.eventloop.common.Script;

import java.util.List;

@Slf4j
public class ReadRedissonOrderEvent extends AbstractEvent<List<CryptoCoin>> {
    public ReadRedissonOrderEvent(List<CryptoCoin> data) {
        super(data);
    }

    @Override
    public void trigger(Script script) {
        script.run(data,
                data -> {
                    // 성공
                },
                (throwable, data) -> {
                    // 실패
                }
        );
    }
}
