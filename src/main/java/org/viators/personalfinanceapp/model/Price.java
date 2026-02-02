package org.viators.personalfinanceapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "prices")
@AllArgsConstructor
@SuperBuilder
public class Price extends BaseEntity {

    @Column(name = "item_amount", nullable = false)
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

}
