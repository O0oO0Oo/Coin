package org.coin.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, Double minimumPrice) {
        super(minimumPrice + errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
