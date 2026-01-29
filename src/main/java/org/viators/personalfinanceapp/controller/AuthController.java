package org.viators.personalfinanceapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.viators.personalfinanceapp.dto.user.request.LoginUserRequest;
import org.viators.personalfinanceapp.dto.user.response.UserAuthResponse;
import org.viators.personalfinanceapp.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(
            @RequestBody @Valid LoginUserRequest request) {

        UserAuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
