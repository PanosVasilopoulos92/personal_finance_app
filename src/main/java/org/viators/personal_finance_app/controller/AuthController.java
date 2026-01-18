package org.viators.personal_finance_app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.viators.personal_finance_app.dto.user.request.CreateUserRequest;
import org.viators.personal_finance_app.dto.user.request.LoginUserRequest;
import org.viators.personal_finance_app.dto.user.response.UserAuthResponse;
import org.viators.personal_finance_app.dto.user.response.UserSummaryResponse;
import org.viators.personal_finance_app.service.AuthService;
import org.viators.personal_finance_app.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(
            @Valid @RequestBody LoginUserRequest request) {

        UserAuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
