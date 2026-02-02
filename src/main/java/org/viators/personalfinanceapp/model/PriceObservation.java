package org.viators.personalfinanceapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.viators.personalfinanceapp.model.enums.CurrencyEnum;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_observations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceObservation extends BaseEntity {

    @Column(name = "price", nullable = false, updatable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, updatable = false)
    private CurrencyEnum currency;

    @Column(name = "observation_date", nullable = false, updatable = false)
    private LocalDate observationDate;

    @Column(name = "location", nullable = false, updatable = false)
    private String location; // city or region

    @Column(name = "notes", length = 400)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;
}
