package org.viators.personalfinanceapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingList extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "shoppingList", fetch = FetchType.LAZY)
    private List<ShoppingListItem> shoppingListItems = new ArrayList<>();
}
