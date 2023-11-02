package com.testcontainers.catalog.domain.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByCode(String code);

    @Modifying
    @Query("update ProductEntity p set p.image = :image where p.code = :code")
    void updateProductImage(@Param("code") String code, @Param("image") String image);
}
