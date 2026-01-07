package org.viators.personal_finance_app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.viators.personal_finance_app.dtos.UserDTOs;
import org.viators.personal_finance_app.model.User;

@Service
@Transactional(readOnly = true)
public class UserService {
}
