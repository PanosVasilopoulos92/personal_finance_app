package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personal_finance_app.model.enums.CurrencyEnum;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "default_currency", nullable = false)
    private CurrencyEnum defaultCurrency;

    @Column(name = "default_location", nullable = false)
    private String defaultLocation;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    @Column(name = "email_alerts", nullable = false)
    private Boolean emailAlerts;

    @Column(name = "preferred_store_ids")
    private String preferredStoreIds;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Overloaded constructor
    public UserPreferences(CurrencyEnum defaultCurrency, String defaultLocation, boolean notificationEnabled, boolean emailAlerts, String preferredStoreIds) {
        super();
        this.defaultCurrency = defaultCurrency;
        this.defaultLocation = defaultLocation;
        this.notificationEnabled = notificationEnabled;
        this.emailAlerts = emailAlerts;
        this.preferredStoreIds = preferredStoreIds;
    }

    public static UserPreferences createDefaultPreferences() {
        return new UserPreferences(
                CurrencyEnum.EUR,
                "",
                true,
                true,
                ""
        );
    }
}
