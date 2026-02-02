package org.viators.personalfinanceapp.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.viators.personalfinanceapp.model.enums.ItemUnitEnum;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Item extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "item_unit")
    @Enumerated(EnumType.STRING)
    private ItemUnitEnum itemUnit;

    @Column(name = "brand")
    private String brand;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;

    @ManyToMany(mappedBy = "items")
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PriceObservation> priceObservations = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceAlert> priceAlerts = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShoppingListItem> shoppingListItems = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceComparison> priceComparisons = new ArrayList<>();

    // Creates a new Join Entity to represent the relationship between Item and Basket, plus some extra field
    @OneToMany(mappedBy = "item", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<BasketItem> basketItems = new ArrayList<>();

    // Helper methods
    public void addPriceObservation(PriceObservation priceObservation) {
        if (priceObservation != null) {
            this.priceObservations.add(priceObservation);
            priceObservation.setItem(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item that)) return false;
        return getUuid() != null && getUuid().equals(that.getUuid());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}