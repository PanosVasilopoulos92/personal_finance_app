package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.viators.personal_finance_app.model.enums.StatusEnum;
import org.viators.personal_finance_app.model.enums.UserRolesEnum;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email", unique = true),
                @Index(name = "idx_user_username", columnList = "username", unique = true),
                @Index(name = "idx_user_lastName", columnList = "lastName", unique = true),
                @Index(name = "idx_user_uuid", columnList = "uuid")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRolesEnum userRole = UserRolesEnum.USER;

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

    public void addUserPreferences(UserPreferences userPreferences) {
        if (userPreferences != null) {
            this.setUserPreferences(userPreferences);
            userPreferences.setUser(this);
        }
    }

    public String getFullName() {
        return this.firstName.concat(" ").concat(this.lastName);
    }

    public boolean isAdmin() {
        return this.userRole.equals(UserRolesEnum.ADMIN);
    }

    public boolean isActive() {
        return this.getStatus().equals(StatusEnum.ACTIVE.getCode());
    }
}