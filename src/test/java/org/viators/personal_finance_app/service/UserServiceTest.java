package org.viators.personal_finance_app.service;

import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.viators.personal_finance_app.dto.user.request.CreateUserRequest;
import org.viators.personal_finance_app.dto.user.response.UserSummaryResponse;
import org.viators.personal_finance_app.exceptions.DuplicateResourceException;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.StatusEnum;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;
import org.viators.personal_finance_app.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Test")
public class UserServiceTest {

    @Mock
    /**
     * We're creating fake versions (mocks) of dependencies that UserService needs.
     * These mocks do nothing by default - they're empty shells
     */
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    /**
     * Mockito creates a real UserService instance
     * It automatically injects our mocks (userRepository, passwordEncoder) into it
     * The magic: We get a real UserService with fake dependencies!
     */
    private UserService userService;

    private User testUser;
    private CreateUserRequest createUserRequest;

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

        createUserRequest = new CreateUserRequest(
                "johndoe",
                "john@example.com",
                "John",
                "Doe",
                "Password123!",
                "Password123!",
                25
        );
    }

    @Test
    @DisplayName("registerUser - valid request - creates user successfully")
    void registerUser_ValidRequest_CreateUser() {
        // Arrange
        // We're telling our mock: "When someone calls existsByEmail(), return false", same logic for the other ones
        when(userRepository.existsByEmail(createUserRequest.email())).thenReturn(false);
        when(userRepository.existsByUsername(createUserRequest.username())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encrypted");

        /*
        // When save() is called with any User object, return our pre-built testUser
        // Simulates successful database save
        // testUser has the ID and UUID that would be set by the database
        // This is what the real repository would do after persisting
         */
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        /*
        This is where the magic happens:
            UserService checks if email exists → mock returns false ✓
            UserService checks if username exists → mock returns false ✓
            UserService encodes password → mock returns "encrypted" ✓
            UserService saves user → mock returns testUser ✓
            UserService maps User to UserSummaryResponse
            Returns response to us
         */
        UserSummaryResponse response = userService.registerUser(createUserRequest);

        System.out.println(response);

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.username()).isEqualTo("johndoe");
        assertThat(response.uuid()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    @DisplayName("Register user - email already exist - throws exception error")
    void registerUser_duplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(createUserRequest.email())).thenReturn(true);

        //Act & Assert
        /**
         * assertThatThrownBy(): Expects the code to throw an exception
         * Lambda syntax () -> ...: Delays execution until AssertJ is ready to catch the exception
         * isInstanceOf(): Verifies the correct exception type
         * hasMessageContaining(): Verifies the error message
         */
        assertThatThrownBy(() -> userService.registerUser(createUserRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email is already in use");

        // Verifies that save() was NEVER called
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivate user - user exists and is active - user deactivated successfully")
    void deactivateUser_UserExistsAndIsActive_UserDeactivated() {
        when(userRepository.findByUuidAndStatus("550e8400-e29b-41d4-a716-446655440000", StatusEnum.ACTIVE.getCode()))
                .thenReturn(Optional.ofNullable(testUser));

        System.out.println(testUser.getStatus());
        UserSummaryResponse response = userService.deactivateUser("550e8400-e29b-41d4-a716-446655440000");
        System.out.println(testUser.getStatus());
        System.out.println(response);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusEnum.INACTIVE.getCode());
    }
}
