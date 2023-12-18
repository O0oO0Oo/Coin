package org.coin.user.dto.response;

import org.coin.user.entity.User;

public record UserDetailResponse(
        Long userId,
        String username,
        Double money
) {

    public static UserDetailResponse of(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getMoney()
        );
    }
}
