package org.viators.personal_finance_app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personal_finance_app.dtos.CreateUserRequest;
import org.viators.personal_finance_app.model.User;

@Service
@Transactional(readOnly = true)
public class UserService {

    public User createUser(CreateUserRequest createUserRequest) {
        if (createUserRequest == null) {

        }
        return new User();
    }
}
