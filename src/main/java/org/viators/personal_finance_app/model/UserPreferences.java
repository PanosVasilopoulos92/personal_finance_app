package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.viators.personal_finance_app.model.enums.CurrencyEnum;
import org.viators.personal_finance_app.model.enums.LanguageEnum;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
public class UserPreferences extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private CurrencyEnum currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private LanguageEnum language;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    @Column(name = "email_alerts", nullable = false)
    private Boolean emailAlerts;

    @ManyToMany()
    @JoinTable(
            name = "user_preferred_stores",
            joinColumns = @JoinColumn(name = "user_preference_id"),
            inverseJoinColumns = @JoinColumn(name = "store_id")
    )
    @Builder.Default
    private Set<Store> preferredStores = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Overloaded constructor
    public UserPreferences(CurrencyEnum currency, String location, boolean notificationEnabled, boolean emailAlerts) {
        super();
        this.currency = currency;
        this.location = location;
        this.notificationEnabled = notificationEnabled;
        this.emailAlerts = emailAlerts;
    }

    public static UserPreferences createDefaultPreferences() {
        return new UserPreferences(
                CurrencyEnum.EUR,
                "",
                false,
                false
        );
    }
}
