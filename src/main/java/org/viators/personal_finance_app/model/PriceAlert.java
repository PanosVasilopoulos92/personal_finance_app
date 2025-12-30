package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personal_finance_app.model.enums.AlertTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertTypeEnum alertTypeEnum;

    @Column(name = "threshold_price")
    private BigDecimal thresholdPrice;

    @Column(name = "percentage_change")
    private BigDecimal percentageChange;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
}
