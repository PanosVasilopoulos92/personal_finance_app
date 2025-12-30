package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
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

    /**
     * @JoinTable tells JPA to create a join table named "basket_items"
     * <p>
     * joinColumns: The foreign key column pointing to THIS entity (Basket)
     * inverseJoinColumns: The foreign key column pointing to the OTHER entity (Item)
     * <p>
     * The join table will have two columns:
     * - basket_id (references baskets.id)
     * - item_id (references items.id)
     */
    @ManyToMany()
    @JoinTable(
            name = "basket_item",
            joinColumns = @JoinColumn(name = "basket_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "item_id", nullable = false)
    )
    private List<Item> items = new ArrayList<>();
}
