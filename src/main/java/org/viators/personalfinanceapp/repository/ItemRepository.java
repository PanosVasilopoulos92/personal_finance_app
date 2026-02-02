package org.viators.personalfinanceapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.viators.personalfinanceapp.model.Category;
import org.viators.personalfinanceapp.model.Item;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByUuid(String uuid);

    Optional<Item> findByUuidAndUser_Uuid(String itemUuid, String userUuid);

    boolean existsByUuidAndStatus(String uuid, String status);

    boolean existsByNameAndUser_IdAndStatusAndCategoriesContaining(String name, Long userId,
                                                                      String status, Category category);

    boolean existsByNameAndUser_IdAndStatusAndCategoriesIsEmpty(String name, Long userId, String status);

    Page<Item> findAllByUser(String userUuid, String status, Pageable pageable);
}
