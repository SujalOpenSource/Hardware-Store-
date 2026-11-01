
package com.hardwarestore.inventory;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<StockMovement> findTop50ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query("""
            SELECT sm
            FROM StockMovement sm
            WHERE (:startDate IS NULL OR sm.createdAt >= :startDate)
              AND (:endDate IS NULL OR sm.createdAt <= :endDate)
            ORDER BY sm.createdAt DESC
            """)
    List<StockMovement> findMovementsForReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
