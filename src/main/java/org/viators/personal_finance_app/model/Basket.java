package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "baskets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Basket extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 800)
    private String description;

    @Column(name = "is_default")
    private Boolean isDefault;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Simple case
//    /**
//     * @JoinTable tells JPA to create a join table named "baskets_items"
//     * <p>
//     * joinColumns: The foreign key column pointing to THIS entity (Basket)
//     * inverseJoinColumns: The foreign key column pointing to the OTHER entity (Item)
//     * <p>
//     * The join table will have two columns:
//     * - basket_id (references baskets.id)
//     * - item_id (references items.id)
//     */
//    @ManyToMany()
//    @JoinTable(
//            name = "baskets_items",
//            joinColumns = @JoinColumn(name = "basket_id", nullable = false),
//            inverseJoinColumns = @JoinColumn(name = "item_id", nullable = false)
//    )
//    private List<Item> items = new ArrayList<>();

    // Approach of creating a new Join Entity to represent the relationship between Item and Basket, plus some extra field
    @OneToMany(mappedBy = "basket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BasketItem> basketItems = new ArrayList<>();

    // Helper methods
    public void addItem(Item item, BigDecimal quontity) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        if (quontity == null || quontity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quontity must be positive");
        }

        BasketItem existingBasketItem = basketItems.stream()
                .filter(bi -> bi.getItem().equals(item))
                .findFirst()
                .orElse(null);

        if (existingBasketItem != null) {
            existingBasketItem.setQuantity(
                    existingBasketItem.getQuantity().add(quontity)
            );
        } else {
            BasketItem basketItem = new BasketItem();
            basketItem.setBasket(this);
            basketItem.setItem(item);
            basketItem.setQuantity(quontity);
        }
    }

    public void addItem(Item item) {
        addItem(item, BigDecimal.ONE);
    }

    public void removeItem(Item item) {
        if (item == null) return;

        basketItems.removeIf(bi -> bi.getItem().equals(item));
    }
}
