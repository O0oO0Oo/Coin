package org.coin.price.event;

public record AsyncSchedulingFailureCountEvent(
        boolean isFailure
) {
    public AsyncSchedulingFailureCountEvent(boolean isFailure) {
        this.isFailure = isFailure;
    }

    public static AsyncSchedulingFailureCountEvent success() {
        return new AsyncSchedulingFailureCountEvent(false);
    }

    public static AsyncSchedulingFailureCountEvent failed() {
        return new AsyncSchedulingFailureCountEvent(true);
    }
}