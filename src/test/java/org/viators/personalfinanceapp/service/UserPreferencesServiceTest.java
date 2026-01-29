package org.viators.personalfinanceapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.viators.personalfinanceapp.dto.userpreferences.request.UpdateUserPrefRequest;
import org.viators.personalfinanceapp.dto.userpreferences.response.UserPreferencesSummaryResponse;
import org.viators.personalfinanceapp.exceptions.ResourceNotFoundException;
import org.viators.personalfinanceapp.model.User;
import org.viators.personalfinanceapp.model.UserPreferences;
import org.viators.personalfinanceapp.model.enums.CurrencyEnum;
import org.viators.personalfinanceapp.model.enums.LanguageEnum;
import org.viators.personalfinanceapp.model.enums.StatusEnum;
import org.viators.personalfinanceapp.model.enums.UserRolesEnum;
import org.viators.personalfinanceapp.repository.StoreRepository;
import org.viators.personalfinanceapp.repository.UserPreferencesRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferences Service Test")
public class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private UserPreferencesService userPreferencesService;

    private User testUser;
    private UserPreferences userPreferences;
    private UpdateUserPrefRequest updateUserPrefRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .uuid("550e8400-e29b-41d4-a716-446655440000")
                .username("johndoe")
                .email("john@example.com")
                .password("encrypted")
                .firstName("John")
                .lastName("Doe")
                .userRole(UserRolesEnum.USER)
                .status(StatusEnum.ACTIVE.getCode())
                .build();

        userPreferences = UserPreferences.builder()
                .id(1L)
                .uuid("550e8400-e29b-41d4-a716-446655440009")
                .language(LanguageEnum.ENGLISH)
                .currency(CurrencyEnum.EUR)
                .notificationEnabled(false)
                .emailAlerts(false)
                .build();

        updateUserPrefRequest = UpdateUserPrefRequest.builder()
                .currency(CurrencyEnum.USD)
                .notificationEnabled(true)
                .emailAlerts(false)
                .build();
    }

    @Test
    void updateUserRequest_validRequest_UpdatePref() {
        // Arrange
        String userUuid = testUser.getUuid();
        when(userPreferencesRepository.findByUser_Uuid(userUuid))
                .thenReturn(Optional.of(userPreferences));

        System.out.println(userPreferences);
        // Act
        UserPreferencesSummaryResponse result = userPreferencesService.updateUserPrefs(userUuid, updateUserPrefRequest);
        System.out.println(result);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.currency()).isEqualTo(CurrencyEnum.USD);
        assertThat(result.notificationEnabled()).isEqualTo(true);
    }

    @Test
    void updateUserPreferences_invalidRequest_ThrowException() {
        // Arrange
        when(userPreferencesRepository.findByUser_Uuid(testUser.getUuid())).thenReturn(Optional.empty());

        // Act && Assert
        assertThatThrownBy(() -> userPreferencesService.updateUserPrefs(testUser.getUuid(), updateUserPrefRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No such user in system.");

        verify(userPreferencesRepository, never()).save(any());
    }
}
