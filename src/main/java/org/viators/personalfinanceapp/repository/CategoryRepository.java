package org.viators.personalfinanceapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.viators.personalfinanceapp.model.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByUuid(String uuid);

    Optional<Category> findByUuidAndUser_UuidAndStatus(String uuid, String userUuid, String status);

    boolean existsByNameAndUser_UuidAndStatus(String name, String uuid, String status);

    Page<Category> findByUser_Uuid(String userUuid, Pageable pageable);

    @Query(value = """
            select c from Category c
            left join fetch c.user
            left join fetch c.items
            where c.user.uuid = :userUuid
            and c.uuid = :categoryUuid
            """)
    Optional<Category> findCategoryWithRelationships(@Param("userUuid") String userUuid,
                                                     @Param("categoryUuid") String categoryUuid);

}
