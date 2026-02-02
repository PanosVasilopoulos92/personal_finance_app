package org.viators.personalfinanceapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.viators.personalfinanceapp.model.PriceObservation;

import java.util.Optional;

@Repository
public interface PriceObservationRepository extends JpaRepository<PriceObservation, Long> {

    @Query("""
            select po from PriceObservation po
            where po.uuid = :uuid
            and po.status = :status
            order by po.createdAt desc
            limit 1
            """)
    Optional<PriceObservation> findLastActivePriceObservation(@Param("uuid") String uuid,
                                                              @Param("status") String status);
}
