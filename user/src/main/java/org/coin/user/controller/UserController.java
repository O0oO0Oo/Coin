package org.coin.user.controller;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.coin.common.request.ApiVersion;
import org.coin.user.dto.request.SignupRequest;
import org.coin.user.dto.response.SignupResponse;
import org.coin.user.dto.response.UserDetailResponse;
import org.coin.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @ApiVersion("1")
    @PostMapping
    public ResponseEntity<SignupResponse> userSignupV1(@Valid @RequestBody SignupRequest signupRequest) {
        SignupResponse signupResponse = userService.addUser(signupRequest);
        return ResponseEntity.created(URI.create("/api/users/" + signupResponse.userId()))
                .body(
                        signupResponse
                );
    }

    @ApiVersion("1")
    @GetMapping("/{user_id}")
    public ResponseEntity<UserDetailResponse> getUserV1(@PathVariable("user_id") Long userId) {
        return ResponseEntity.ok().body(userService.findUser(userId));
    }
}