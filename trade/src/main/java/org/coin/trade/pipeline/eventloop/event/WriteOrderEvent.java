package org.coin.trade.pipeline.eventloop.event;

import lombok.extern.slf4j.Slf4j;
import org.coin.trade.dto.pipeline.event.reader.ReadOrderDto;
import org.coin.trade.pipeline.eventloop.script.Script;

@Slf4j
public class WriteOrderEvent extends AbstractEvent<ReadOrderDto> {
    public WriteOrderEvent(ReadOrderDto data) {
        super(data);
    }

    @Override
    public void trigger(Script<ReadOrderDto> script) {
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
