package org.coin.trade.pipeline.eventloop.event;

import lombok.extern.slf4j.Slf4j;
import org.coin.trade.dto.pipeline.async.writer.WriteOrderDto;
import org.coin.trade.pipeline.eventloop.script.Script;

@Slf4j
public class ProcessedOrderEvent extends AbstractEvent<WriteOrderDto> {
    public ProcessedOrderEvent(WriteOrderDto data) {
        super(data);
    }


    @Override
    public void trigger(Script<WriteOrderDto> script) {
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
