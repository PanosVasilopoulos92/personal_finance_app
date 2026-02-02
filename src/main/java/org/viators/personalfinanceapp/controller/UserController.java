package org.viators.personalfinanceapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.viators.personalfinanceapp.dto.user.request.CreateUserRequest;
import org.viators.personalfinanceapp.dto.user.request.UpdateUserRequest;
import org.viators.personalfinanceapp.dto.user.response.UserDetailsResponse;
import org.viators.personalfinanceapp.dto.user.response.UserSummaryResponse;
import org.viators.personalfinanceapp.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
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

    @PutMapping("/{uuid}/update")
    public ResponseEntity<UserSummaryResponse> updateUser(@PathVariable String uuid, @RequestBody UpdateUserRequest request) {
        UserSummaryResponse response = userService.updateUserInfo(uuid, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{uuid}/deactivate")
    @PreAuthorize("@userSecurity.isSelf(#uuid)")
    public ResponseEntity<Void> deactivateUser(@PathVariable String uuid) {
        userService.deactivateUser(uuid);
        return ResponseEntity.noContent().build();
    }
}
