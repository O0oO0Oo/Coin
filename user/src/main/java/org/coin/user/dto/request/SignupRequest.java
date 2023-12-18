package org.coin.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record SignupRequest(
        @NotNull(message = "이름은 필수 입력 값입니다")
        @Length(min = 6, max = 20, message = "이름은 6 ~ 20자 입니다.")
        String username,
        @NotNull(message = "비밀번호는 필수 입력 값입니다.")
        @Length(min = 8,message = "비밀번호는 8자 이상입니다.")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z!@#$%^&*()-_=+]).+$", message = "비밀번호는 숫자, 문자, 특수문자 중 2가지 이상을 포함해야 합니다.")
        String password
) {
}