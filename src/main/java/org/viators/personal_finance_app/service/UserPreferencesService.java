package org.viators.personal_finance_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personal_finance_app.dto.userpreferences.request.UpdatePreferredStores;
import org.viators.personal_finance_app.dto.userpreferences.request.UpdateUserPrefRequest;
import org.viators.personal_finance_app.dto.userpreferences.response.UserPreferencesSummary;
import org.viators.personal_finance_app.exceptions.ResourceNotFoundException;
import org.viators.personal_finance_app.model.Store;
import org.viators.personal_finance_app.model.UserPreferences;
import org.viators.personal_finance_app.repository.StoreRepository;
import org.viators.personal_finance_app.repository.UserPreferencesRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final StoreRepository storeRepository;

    public UserPreferencesSummary getPreferences(String uuid) {
        UserPreferences userPreferences = userPreferencesRepository.findByUser_Uuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No such user in system."));

        return UserPreferencesSummary.from(userPreferences);
    }

    @Transactional
    public UserPreferencesSummary updateUserPrefs(String uuid, UpdateUserPrefRequest request) {
        UserPreferences userPreferencesToUpdate = userPreferencesRepository.findByUser_Uuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No such user in system."));

        request.updateUserPrefs(userPreferencesToUpdate);
        return UserPreferencesSummary.from(userPreferencesToUpdate);
    }

    @Transactional
    public UserPreferencesSummary resetUserPrefsToDefault(String uuid) {
        UserPreferences userPreferencesToUpdate = userPreferencesRepository.findByUser_Uuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No such user in system."));

        UpdateUserPrefRequest.resetUserPrefs(userPreferencesToUpdate);
        return UserPreferencesSummary.from(userPreferencesToUpdate);
    }

    @Transactional
    public void updateUserPreferredStores(String uuid, UpdatePreferredStores request) {
        UserPreferences userPreferences = userPreferencesRepository.findByUser_Uuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with this uuid"));

        Store storeToUpdate = storeRepository.findByUuid(request.uuid())
                .orElseThrow(() -> new ResourceNotFoundException("No store found with this uuid"));

        if (userPreferences.getPreferredStores().contains(storeToUpdate)) {
            userPreferences.getPreferredStores().remove(storeToUpdate);
        } else {
            userPreferences.getPreferredStores().add(storeToUpdate);
        }
    }

}
