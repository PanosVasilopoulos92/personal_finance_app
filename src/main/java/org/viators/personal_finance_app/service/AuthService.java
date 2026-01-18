package org.viators.personal_finance_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.viators.personal_finance_app.dto.user.request.LoginUserRequest;
import org.viators.personal_finance_app.dto.user.response.UserAuthResponse;
import org.viators.personal_finance_app.exceptions.InvalidCredentialsException;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.security.JwtUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserAuthResponse login(LoginUserRequest request) {

        User userToAuthenticate = userService.findUserByEmail(request.email());

        if (userToAuthenticate == null) {
            throw new InvalidCredentialsException("No user found with this email");
        }

        if (!passwordEncoder.matches(request.password(), userToAuthenticate.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!userToAuthenticate.isActive()) {
            throw new InvalidCredentialsException("User is deactivated");
        }

        String token = jwtUtil.generateToken(userToAuthenticate);

        log.info("Login successful for user: {}", userToAuthenticate.getId());

        return UserAuthResponse.of(token, userToAuthenticate, jwtUtil.getExpiration());
    }
}
