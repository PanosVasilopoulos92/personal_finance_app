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
@SuperBuilder()
public class Category extends BaseEntity {

    @Column(name = "category_name", nullable = false, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<InflationReport> inflationReports = new ArrayList<>();

    public void addUser(User user) {
        if (user != null) {
            this.user = user;
            this.user.getCategories().add(this);
        }
    }
}
