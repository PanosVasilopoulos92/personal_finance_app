package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the personal finance application.
 *
 * Business Rules Enforced:
 * - Email must be unique (database constraint + service validation)
 * - Password is always encrypted (never stored in plain text)
 * - New users get USER role by default
 * - Users can be soft-deleted (active = false)
 */
@Entity
@Table(
        name = "users",
        indexes = @Index(name = "idx_user_email", columnList = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "firstname")
    private String firstName;

    @Column(name = "lastname")
    private String lastName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRolesEnum userRole;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences userPreferences;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PriceAlert> priceAlerts = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ShoppingList> shoppingLists = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<InflationReport> inflationReports = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PriceComparison> priceComparisons = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Basket> baskets = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return getUuid() != null && getUuid().equals(that.getUuid());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Helper methods
    public void addItem(Item item) {
        if (item != null) {
            this.items.add(item);
            item.setUser(this);
        }
    }

    public void addCategory(Category category) {
        if (category != null) {
            this.categories.add(category);
            category.setUser(this);
        }
    }

    public String getFullName() {
        return this.firstName.concat(" ").concat(this.lastName);
    }

    public boolean isAdmin() {
        return this.userRole.equals(UserRolesEnum.ADMIN);
    }
}