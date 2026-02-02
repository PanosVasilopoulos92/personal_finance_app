package org.viators.personalfinanceapp.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class Category extends BaseEntity {

    @Column(name = "category_name", nullable = false, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
            name = "categories_items",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<InflationReport> inflationReports = new ArrayList<>();

    // Helper methods
    public void addUser(User user) {
        if (user != null) {
            this.user = user;
            this.user.getCategories().add(this);
        }
    }

    public void addItem(Item item) {
        if (item != null) {
            this.items.add(item);
            item.getCategories().add(this);
        }
    }

}
