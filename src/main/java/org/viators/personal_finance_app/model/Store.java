package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personal_finance_app.model.enums.StoreTypeEnum;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "stores",
        indexes = @Index(
                name = "idx_store_name", columnList = "store_name"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Store extends BaseEntity {

    @Column(name = "store_name", nullable = false, unique = true)
    private String name;

    @Column(name = "store_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StoreTypeEnum storeType;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "region")
    private String region;

    @Column(name = "country")
    private String country;

    @Column(name = "website")
    private String website;

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<PriceObservation> priceObservation;

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<ShoppingListItem> shoppingListItems = new ArrayList<>();

    @OneToMany(mappedBy = "bestStore", fetch = FetchType.LAZY)
    private List<PriceComparison> priceComparisons = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Store that)) return false;
        return getUuid() != null && getUuid().equals(that.getUuid());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
