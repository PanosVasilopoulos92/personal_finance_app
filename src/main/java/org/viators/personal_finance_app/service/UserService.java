package org.viators.personal_finance_app.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personal_finance_app.dto.user.request.CreateUserRequest;
import org.viators.personal_finance_app.dto.user.request.UpdateUserPasswordRequest;
import org.viators.personal_finance_app.dto.user.request.UpdateUserRequest;
import org.viators.personal_finance_app.dto.user.response.UserDetailsResponse;
import org.viators.personal_finance_app.dto.user.response.UserSummaryResponse;
import org.viators.personal_finance_app.exceptions.BusinessException;
import org.viators.personal_finance_app.exceptions.DuplicateResourceException;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.UserPreferences;
import org.viators.personal_finance_app.model.enums.StatusEnum;
import org.viators.personal_finance_app.repository.UserRepository;

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
            throw new EntityNotFoundException(String.format("User with uuid: %s does not exist", uuid));
        }

        userToUpdate.setPassword(encryptPassword(request.newPassword()));
        return true;
    }

    @Transactional
    public UserSummaryResponse updateUserInfo(String uuid, UpdateUserRequest updateUserRequest) {
        User userToUpdate = userRepository.findByUuidAndStatus(uuid, StatusEnum.ACTIVE.getCode()).orElse(null);

        if (userToUpdate == null) {
            throw new EntityNotFoundException(String.format("User with uuid: %s does not exist", uuid));
        }

        updateUserRequest.updateUser(userToUpdate); // No need to call save() - dirty checking handles it!
        return UserSummaryResponse.from(userToUpdate);
    }

    @Transactional
    public UserSummaryResponse deactivateUser(String uuid) {
        User userToDeactivate = userRepository.findByUuidAndStatus(uuid, StatusEnum.ACTIVE.getCode())
                .orElseThrow(() -> new EntityNotFoundException("User does not exist or is already deactivated"));

        userToDeactivate.setStatus(StatusEnum.INACTIVE.getCode());
        return UserSummaryResponse.from(userToDeactivate);
    }

    @Transactional(readOnly = true)
    public User findUserByUuidAndStatus(String uuid, StatusEnum status) {
        return userRepository.findByUuidAndStatus(uuid, status.getCode()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> findAllUsers(String uuid) {
        User user = userRepository.findByUuid(uuid).orElseThrow(() ->
                new IllegalArgumentException("Request made for a user that not exist."));

        if (!user.isAdmin()) {
            throw new BusinessException("User cannot see other users unless is an admin user");
        }

        return userRepository.findAll().stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> findAllUsersPaginated(int page, int size, String sortBy, String direction) {

        log.debug("Fetching users - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
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
                .orElseThrow(() -> new EntityNotFoundException("No user found with this uuid"));

        return UserDetailsResponse.from(result);
    }

    public UserSummaryResponse findUserByUuid(String uuid) {
        User result =  userRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("No user found with this uuid"));

        return UserSummaryResponse.from(result);
    }

}


