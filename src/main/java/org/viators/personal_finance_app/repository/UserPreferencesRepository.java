package org.viators.personal_finance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.viators.personal_finance_app.model.UserPreferences;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    // The underscore (`_`), "traversal delimiter", explicitly tells Spring Data JPA to traverse into a nested entity.
    // It's resolving this path: UserPreferences.user.uuid
    Optional<UserPreferences> findByUser_Uuid(String uuid);

}
