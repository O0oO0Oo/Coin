package org.coin.trade.pipeline.eventloop.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEventHandler implements EventHandler {
    protected final Map<Class<? extends Event>, Script> eventScriptMap = new ConcurrentHashMap<>();

    public void addScript(Event event, Script script) {
        this.eventScriptMap.put(event.getClass(), script);
    }

    public void removeScript(Event event) {
        this.eventScriptMap.remove(event.getClass());
    }
}