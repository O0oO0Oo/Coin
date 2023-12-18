package org.coin.order.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 총 주문금액 validation
 */
@Documented
@Constraint(validatedBy = TotalOrderPriceValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTotalOrderPrice {
    String message() default "총 주문 금액은 1000원 이상이어야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
