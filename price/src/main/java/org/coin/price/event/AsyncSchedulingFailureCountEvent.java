package org.coin.price.event;

import lombok.Getter;

public class AsyncSchedulingFailureCountEvent {
    @Getter
    private boolean isFailure;

    public AsyncSchedulingFailureCountEvent(boolean flag) {
        this.isFailure = flag;
    }

    public static AsyncSchedulingFailureCountEvent success() {
        return new AsyncSchedulingFailureCountEvent(false);
    }

    public static AsyncSchedulingFailureCountEvent failed() {
        return new AsyncSchedulingFailureCountEvent(true);
    }
}