package org.viators.personalfinanceapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personalfinanceapp.dto.user.request.CreateUserRequest;
import org.viators.personalfinanceapp.dto.user.request.UpdateUserPasswordRequest;
import org.viators.personalfinanceapp.dto.user.request.UpdateUserRequest;
import org.viators.personalfinanceapp.dto.user.response.UserDetailsResponse;
import org.viators.personalfinanceapp.dto.user.response.UserSummaryResponse;
import org.viators.personalfinanceapp.exceptions.BusinessException;
import org.viators.personalfinanceapp.exceptions.DuplicateResourceException;
import org.viators.personalfinanceapp.exceptions.ResourceNotFoundException;
import org.viators.personalfinanceapp.model.User;
import org.viators.personalfinanceapp.model.UserPreferences;
import org.viators.personalfinanceapp.model.enums.StatusEnum;
import org.viators.personalfinanceapp.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor // used for DI
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserSummaryResponse registerUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already in use");
        }

        if (request.age() < 13) {
            throw new IllegalArgumentException("User must be above 13 years old in order to register.");
        }

        User userToRegister = request.toEntity();
        userToRegister.setPassword(encryptPassword(request.password()));

        //Create default Preferences
        UserPreferences userPreferences = UserPreferences.createDefaultPreferences();
        userToRegister.addUserPreferences(userPreferences);

        UserSummaryResponse userCreated = UserSummaryResponse.from(userRepository.save(userToRegister));
        log.info("Successfully registered user with uuid: {}", userCreated.uuid());

        return userCreated;
    }

    private String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Transactional
    public boolean updateUserPassword(String uuid, UpdateUserPasswordRequest request) {
        User userToUpdate = userRepository.findByUuidAndStatus(uuid, StatusEnum.ACTIVE.getCode()).orElse(null);

        if (userToUpdate == null) {
            throw new ResourceNotFoundException(String.format("User with uuid: %s does not exist", uuid));
        }

        userToUpdate.setPassword(encryptPassword(request.newPassword()));
        return true;
    }

    @Transactional
    public UserSummaryResponse updateUserInfo(String uuid, UpdateUserRequest updateUserRequest) {
        User userToUpdate = userRepository.findByUuidAndStatus(uuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User with uuid: %s does not exist or is inactive", uuid)));

        updateUserRequest.updateUser(userToUpdate); // No need to call save() - dirty checking handles it!
        return UserSummaryResponse.from(userToUpdate);
    }

    @Transactional
    public void deactivateUser(String uuid) {
        User userToDeactivate = userRepository.findByUuidAndStatus(uuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist or is already deactivated"));

        userToDeactivate.setStatus(StatusEnum.INACTIVE.getCode());
    }

    @Transactional(readOnly = true)
    public User findUserByUuidAndStatus(String uuid, StatusEnum status) {
        return userRepository.findByUuidAndStatus(uuid, status.getCode()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> findAllUsers(String uuid) {
        User user = userRepository.findByUuid(uuid).orElseThrow(() ->
                new ResourceNotFoundException("Request made from a user that does not exist."));

        if (!user.isAdmin()) {
            throw new BusinessException("User cannot see other users unless is an admin user");
        }

        return userRepository.findAll().stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> findAllUsersPaginated(Pageable pageable) {

        Page<User> users= userRepository.findAll(pageable);

        return users.map(UserSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return userRepository.existsByEmail(email);
    }

    // Used for auth request in auth service
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public UserDetailsResponse findUserByUuidWithAllRelationships(String uuid) {
        User result = userRepository.findUserByUuidWithAllRelationships(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with this uuid"));

        return UserDetailsResponse.from(result);
    }

    public UserSummaryResponse findUserByUuid(String uuid) {
        User result =  userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with this uuid"));

        return UserSummaryResponse.from(result);
    }

}