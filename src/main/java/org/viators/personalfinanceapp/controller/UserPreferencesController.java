package org.viators.personalfinanceapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.viators.personalfinanceapp.dto.userpreferences.request.UpdatePreferredStoresRequest;
import org.viators.personalfinanceapp.dto.userpreferences.request.UpdateUserPrefRequest;
import org.viators.personalfinanceapp.dto.userpreferences.response.UserPreferencesSummaryResponse;
import org.viators.personalfinanceapp.repository.UserRepository;
import org.viators.personalfinanceapp.service.UserPreferencesService;

@RestController
@RequestMapping("api/v1/user-preferences")
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;
    private final UserRepository userRepository;

    @GetMapping("/{uuid}")
    public ResponseEntity<UserPreferencesSummaryResponse> getUserPreferences(@PathVariable String uuid) {
        UserPreferencesSummaryResponse response = userPreferencesService.getPreferences(uuid);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<UserPreferencesSummaryResponse> updateUserPreferences(
            @PathVariable String uuid,
            @RequestBody @Valid UpdateUserPrefRequest request) {
        UserPreferencesSummaryResponse response = userPreferencesService.updateUserPrefs(uuid, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{uuid}/reset")
    public ResponseEntity<UserPreferencesSummaryResponse> resetToDefault(@PathVariable String uuid) {
        return ResponseEntity.ok(userPreferencesService.resetUserPrefsToDefault(uuid));
    }

    @PutMapping("/{uuid}/update-favorite-stores")
    public ResponseEntity<Void> updateFavoriteStores(@PathVariable() String uuid,
                                                     @RequestBody @Valid UpdatePreferredStoresRequest request) {
        userPreferencesService.updateUserPreferredStores(uuid, request);
        return ResponseEntity.noContent().build();
    }
}
