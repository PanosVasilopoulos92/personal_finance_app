package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "baskets_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_basket_item",
                columnNames = {"basket_id", "item_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasketItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "basket_id", nullable = false)
    private Basket basket;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    // Helper methods

    public void setBasket(Basket basket) {
        if (this.basket != null) {
            this.basket.getBasketItems().remove(this);
        }

        this.basket = basket;

        if (basket != null && !basket.getBasketItems().contains(this)) {
            basket.getBasketItems().add(this);
        }
    }

    public void setItem(Item item) {
        if (this.item != null) {
            this.item.getBasketItems().remove(this);
        }

        this.item = item;

        if (item != null && !item.getBasketItems().contains(this)) {
            item.getBasketItems().add(this);
        }
    }
}
