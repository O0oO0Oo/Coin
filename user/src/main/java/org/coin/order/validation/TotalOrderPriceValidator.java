package org.coin.order.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.coin.order.dto.request.AddOrderRequest;
import org.springframework.beans.factory.annotation.Value;

public class TotalOrderPriceValidator implements ConstraintValidator<ValidTotalOrderPrice, AddOrderRequest> {

    @Value("${user.api.minimum-order-price}")
    private Double minimumOrderPrice;

    @Override
    public void initialize(ValidTotalOrderPrice constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(AddOrderRequest value, ConstraintValidatorContext context) {
        if(value.price() != null && value.quantity() != null){
            return value.price() * value.quantity() >= minimumOrderPrice;
        }
        return false;
    }
}
