
package com.hardwarestore.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll(Sort sort);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p
            FROM Product p
            WHERE (
                :search IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (
                :categoryId IS NULL
                OR p.category.id = :categoryId
            )
            """)
    List<Product> searchProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Sort sort
    );

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.stockQuantity <= p.minimumStock
            ORDER BY p.name ASC
            """)
    List<Product> findLowStockProducts();

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p")
    List<Product> findAllWithCategory(Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
