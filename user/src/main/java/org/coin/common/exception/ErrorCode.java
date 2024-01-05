package org.coin.common.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // user
    USER_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "U001", "이미 존재하는 이름입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "유저를 찾을 수 없습니다."),

    // crypto
    CRYPTO_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않는 암호화페 입니다."),
    CRYPTO_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "C002", "거래 불가능한 암호화폐 입니다."),

    // wallet
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "지갑을 찾을 수 없습니다."),

    // buy order
    NOT_ENOUGH_MONEY(HttpStatus.BAD_REQUEST,"B001", "잔액이 부족합니다."),

    // sell order
    NOT_ENOUGH_CRYPTO(HttpStatus.BAD_REQUEST, "S001", "보유 암호화폐가 부족합니다."),

    // order common
    ALREADY_PROCESSED(HttpStatus.BAD_REQUEST,"O003", "이미 거래된 주문입니다. 취소할 수 없습니다."),
    ALREADY_CANCELED(HttpStatus.BAD_REQUEST,"O004", "이미 취소된 주문입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O002", "주문을 찾을 수 없습니다."),
    TOTAL_ORDER_PRICE_INVALID(HttpStatus.BAD_REQUEST, "O005", "원 이상을 주문해야 합니다."),
    // TODO : redis 에서 삭제를 못하면?
    ORDER_CANT_BE_CANCELED(HttpStatus.INTERNAL_SERVER_ERROR, "O006", "주문을 취소할 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
