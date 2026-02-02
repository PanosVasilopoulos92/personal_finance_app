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

    @ManyToMany(mappedBy = "items")
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @ManyToMany(mappedBy = "items")
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceObservation> priceObservation = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceAlert> priceAlerts = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShoppingListItem> shoppingListItems = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceComparison> priceComparisons = new ArrayList<>();

    // Approach of creating a new Join Entity to represent the relationship between Item and Basket, plus some extra field
    @OneToMany(mappedBy = "item", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<BasketItem> basketItems = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<Price> prices;

    // Helper methods
    public void addUser(User user) {
        if (user != null) {
            this.users.add(user);
            user.getItems().add(this);
        }
    }

    public void addPriceObservation(PriceObservation priceObservation) {
        if (priceObservation != null) {
            this.priceObservation.add(priceObservation);
            priceObservation.setItem(this);
        }
    }
}