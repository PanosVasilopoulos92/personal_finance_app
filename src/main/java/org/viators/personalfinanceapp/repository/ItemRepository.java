package org.viators.personalfinanceapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.viators.personalfinanceapp.model.Item;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByUuid(String uuid);

    boolean existsByUuidAndStatus(String uuid, String status);
}
