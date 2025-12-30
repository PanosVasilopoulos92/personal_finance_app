package org.viators.personal_finance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.viators.personal_finance_app.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
