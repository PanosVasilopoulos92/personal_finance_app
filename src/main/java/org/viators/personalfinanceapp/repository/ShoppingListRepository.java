package org.viators.personalfinanceapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.viators.personalfinanceapp.dto.shoppinglist.response.ShoppingListSummaryResponse;
import org.viators.personalfinanceapp.model.ShoppingList;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    Optional<ShoppingList> findByUuidAndStatus(String uuid, String status);

    @Query("""
            SELECT new org.viators.personalfinanceapp.dto.shoppinglist.response.ShoppingListSummaryResponse(
                sl.uuid, sl.name, sl.description, SIZE(sl.shoppingListItems)
            )
            FROM ShoppingList sl
            WHERE sl.user.uuid = :userUuid AND sl.status = :status
            """)
    List<ShoppingListSummaryResponse> findAllSummariesByUserUuidAndStatus(
            @Param("userUuid") String userUuid,
            @Param("status") String status);
}
