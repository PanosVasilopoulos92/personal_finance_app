package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personal_finance_app.model.enums.ReportType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inflation_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InflationReport extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "inflation_rate")
    private BigDecimal inflationRate;

    @Column(name = "average_price")
    private BigDecimal averagePrice;

    @Column(name = "price_change_amount")
    private BigDecimal priceChangeAmount;

    @Column(name = "item_count")
    private Integer itemCount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
