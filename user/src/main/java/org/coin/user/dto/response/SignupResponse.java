package org.coin.user.dto.response;

import org.coin.user.entity.User;

public record SignupResponse(
        Long userId,
        String username
) {
    public static SignupResponse of(User user) {
        return new SignupResponse(user.getId(), user.getUsername());
    }
}