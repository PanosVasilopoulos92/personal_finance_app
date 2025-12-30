package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Links items to shopping lists with quantity information.
 */
@Entity
@Table(name = "shopping_list_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListItem extends BaseEntity {

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "is_purchased", nullable = false)
    private Boolean isPurchased;

    @Column(name = "purchased_price")
    private BigDecimal purchasedPrice;

    @Column(name = "purchased_date")
    private LocalDate purchasedDate;

    @ManyToOne
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "store", nullable = false)
    private Store store;

}
