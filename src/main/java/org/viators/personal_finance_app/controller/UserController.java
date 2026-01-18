package org.viators.personal_finance_app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.viators.personal_finance_app.dto.user.request.CreateUserRequest;
import org.viators.personal_finance_app.dto.user.response.UserDetailsResponse;
import org.viators.personal_finance_app.dto.user.response.UserSummaryResponse;
import org.viators.personal_finance_app.service.UserService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserSummaryResponse> register(
            @Valid @RequestBody CreateUserRequest request) {
        UserSummaryResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{uuid}/details")
    public ResponseEntity<UserDetailsResponse> getUserWithDetails(@PathVariable String uuid) {
        UserDetailsResponse response = userService.findUserByUuidWithAllRelationships(uuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<UserSummaryResponse> getUser(@PathVariable String uuid) {
        UserSummaryResponse response = userService.findUserByUuid(uuid);
        return ResponseEntity.ok(response);
    }
}
