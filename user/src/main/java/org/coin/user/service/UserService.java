package org.coin.user.service;

import lombok.RequiredArgsConstructor;
import org.coin.common.exception.CustomException;
import org.coin.common.exception.ErrorCode;
import org.coin.user.dto.request.SignupRequest;
import org.coin.user.dto.response.SignupResponse;
import org.coin.user.dto.response.UserDetailResponse;
import org.coin.user.entity.User;
import org.coin.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public SignupResponse addUser(SignupRequest request) {
        checkUniqueUsername(request.username());

        User user = User.builder()
                .username(request.username())
                .password(request.password())
                .build();

        User savedUser = userRepository.save(user);
        return SignupResponse.of(savedUser);
    }

    private void checkUniqueUsername(String username) {
        userRepository.findByUsername(username)
                .ifPresent(user -> {
                    throw new CustomException(ErrorCode.USER_ALREADY_EXIST);
                });
    }

    @Transactional(readOnly = true)
    public UserDetailResponse findUser(Long userId) {
        User user = findUserByIdOrElseThrow(userId);

        return UserDetailResponse.of(user);
    }

    private User findUserByIdOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
