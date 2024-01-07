package org.coin.trade.pipeline.eventloop.write;

import org.coin.trade.dto.pipeline.event.reader.ReadOrderDto;
import org.coin.trade.pipeline.eventloop.common.AbstractEvent;
import org.coin.trade.pipeline.eventloop.common.Script;

public class WriterEvent extends AbstractEvent<ReadOrderDto> {
    public WriterEvent(ReadOrderDto data) {
        super(data);
    }

    @Override
    public void trigger(Script<ReadOrderDto> script) {

    }
}
