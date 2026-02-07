package org.viators.personalfinanceapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.viators.personalfinanceapp.dto.shoppinglistitem.response.ShoppingListItemSummaryResponse;
import org.viators.personalfinanceapp.model.ShoppingListItem;

import java.util.Optional;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    Optional<ShoppingListItem> findByUuidAndStatus(String uuid, String status);

    @Query("""
            SELECT new org.viators.personalfinanceapp.dto.shoppinglistitem.response.ShoppingListItemSummaryResponse(
                sli.uuid,
                sli.item.uuid,
                sli.item.name,
                sli.item.brand,
                sli.item.itemUnit,
                sli.store.uuid,
                sli.store.name,
                sli.quantity,
                sli.isPurchased,
                sli.purchasedPrice,
                sli.purchasedDate
            )
            FROM ShoppingListItem sli
            JOIN sli.item
            JOIN sli.store
            WHERE sli.uuid = :uuid AND sli.status = :status
            """)
    Optional<ShoppingListItemSummaryResponse> findShoppingListItemWithRelations(
            @Param("uuid") String uuid,
            @Param("status") String status);
}
