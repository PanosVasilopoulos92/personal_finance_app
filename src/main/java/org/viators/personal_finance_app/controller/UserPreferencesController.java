package org.viators.personal_finance_app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.viators.personal_finance_app.dto.userpreferences.request.UpdatePreferredStores;
import org.viators.personal_finance_app.dto.userpreferences.request.UpdateUserPrefRequest;
import org.viators.personal_finance_app.dto.userpreferences.response.UserPreferencesSummary;
import org.viators.personal_finance_app.repository.UserRepository;
import org.viators.personal_finance_app.service.UserPreferencesService;

@RestController
@RequestMapping("api/user-preferences")
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;
    private final UserRepository userRepository;

    @GetMapping("/{uuid}")
    public ResponseEntity<UserPreferencesSummary> getUserPreferences(@PathVariable String uuid) {
        UserPreferencesSummary response = userPreferencesService.getPreferences(uuid);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<UserPreferencesSummary> updateUserPreferences(
            @PathVariable String uuid,
            @RequestBody @Valid UpdateUserPrefRequest request) {
        UserPreferencesSummary response = userPreferencesService.updateUserPrefs(uuid, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{uuid}/reset")
    public ResponseEntity<UserPreferencesSummary> resetToDefault(@PathVariable String uuid) {
        return ResponseEntity.ok(userPreferencesService.resetUserPrefsToDefault(uuid));
    }

    @PutMapping("/{uuid}/update-favorite-stores")
    public ResponseEntity<Void> updateFavoriteStores(@PathVariable() String uuid,
                                                     @RequestBody @Valid UpdatePreferredStores request) {
        userPreferencesService.updateUserPreferredStores(uuid, request);
        return ResponseEntity.noContent().build();
    }
}
