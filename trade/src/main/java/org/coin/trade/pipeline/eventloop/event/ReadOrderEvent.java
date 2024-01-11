package org.coin.trade.pipeline.eventloop.event;

import lombok.extern.slf4j.Slf4j;
import org.coin.price.dto.CryptoCoin;
import org.coin.trade.pipeline.eventloop.script.Script;

import java.util.List;

@Slf4j
public class ReadOrderEvent extends AbstractEvent<List<CryptoCoin>> {
    public ReadOrderEvent(List<CryptoCoin> data) {
        super(data);
    }

    @Override
    public void trigger(Script<List<CryptoCoin>> script) {
        script.run(data,
                data -> {
                    // 성공
                    log.info("{} processed.", data);
                },
                (throwable, data) -> {
                    // 실패
                    log.error(throwable.getMessage() + " at " + data);
                }
        );
    }
}