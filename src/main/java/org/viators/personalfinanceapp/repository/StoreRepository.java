package org.viators.personalfinanceapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.viators.personalfinanceapp.model.Store;

import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByUuid(String uuid);

    Optional<Store> findByName(String name);
}
