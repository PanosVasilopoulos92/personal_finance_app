package org.viators.personal_finance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personal_finance_app.model.enums.CurrencyEnum;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_observations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceObservation extends BaseEntity {

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private CurrencyEnum currency;

    @Column(name = "observation_date", nullable = false)
    private LocalDate observationDate;

    @Column(name = "location", nullable = false)
    private String location; // city or region

    @Column(name = "notes", length = 800)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
}
