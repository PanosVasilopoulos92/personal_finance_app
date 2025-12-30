package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personal_finance_app.model.enums.ItemUnitEnum;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    private List<PriceObservation> priceObservation = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    private List<PriceAlert> priceAlerts = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    private List<ShoppingListItem> shoppingListItems = new ArrayList<>();

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    private List<PriceComparison> priceComparisons = new ArrayList<>();

    /**
     * mappedBy = "items" tells JPA that the Basket entity owns the relationship.
     * The "items" refers to the field name in the Basket class.
     * <p>
     * This is the "inverse side" or "non-owning side" of the relationship.
     * JPA will not create another join table - it uses the one defined in Basket.
     */
    @ManyToMany(mappedBy = "items")
    private List<Basket> baskets = new ArrayList<>();

    // Helper methods
    public void addPriceObservation(PriceObservation priceObservation) {
        if (priceObservation != null) {
            this.priceObservation.add(priceObservation);
            priceObservation.setItem(this);
        }
    }
}