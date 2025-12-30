package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_comparisons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceComparison extends BaseEntity {

    @Column(name = "comparison_date", nullable = false)
    private LocalDate comparisonDate;

    @Column(name = "lowest_price")
    private BigDecimal lowestPrice;

    @Column(name = "highest_price")
    private BigDecimal highestPrice;

    @Column(name = "average_price")
    private BigDecimal averagePrice;

    @Column(name = "price_spread")
    private BigDecimal priceSpread;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "best_store_id", nullable = false)
    private Store bestStore;
}
