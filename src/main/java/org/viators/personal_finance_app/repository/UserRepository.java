package org.viators.personal_finance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.viators.personal_finance_app.model.User;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(String uuid);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUuidAndStatus(String uuid, String status);

    int countAllByUserRoleAndStatus(UserRolesEnum userRole, String status);

    @Query("""
            select u from User u
            where lower(u.lastName) = lower(concat('%', :lastName, '%'))
            """)
    List<User> searchUserByLastName(@Param("lastName") String lastName);

    @Query("""
            select u from User u
            where u.createdAt between :dateFrom and :dateTo
            order by u.createdAt""")
    List<User> findUsersCreatedBetweenDates(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    @Query("""
            select u from User u
            left join fetch u.inflationReports
            left join fetch u.baskets
            left join fetch u.categories
            left join fetch u.items
            left join fetch u.priceAlerts
            left join fetch u.shoppingLists
            left join fetch u.priceComparisons
            left join fetch u.userPreferences
            where u.uuid = :uuid""")
    Optional<User> findUserByUuidWithAllRelationships(@Param("uuid") String uuid);
}