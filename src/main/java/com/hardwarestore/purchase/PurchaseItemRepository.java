
        package com.hardwarestore.purchase;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    @Query("""
        SELECT COALESCE(SUM(pi.quantity), 0)
        FROM PurchaseItem pi
        WHERE pi.purchase.status = :status
          AND pi.purchase.purchaseDate >= :startDate
          AND pi.purchase.purchaseDate <= :endDate
        """)
    BigDecimal calculatePurchasedQuantity(
            @Param("status") PurchaseStatus status,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );
}
