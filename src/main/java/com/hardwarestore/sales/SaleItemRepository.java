
package com.hardwarestore.sales;

import com.hardwarestore.sales.SaleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @EntityGraph(attributePaths = "product")
    List<SaleItem> findBySaleIdOrderByIdAsc(Long saleId);

    @Query("""
            SELECT item
            FROM SaleItem item
            JOIN FETCH item.product
            JOIN FETCH item.sale
            WHERE item.sale.status = :status
              AND item.sale.saleDate >= :startDate
              AND item.sale.saleDate < :endDate
            """)
    List<SaleItem> findItemsForDashboard(
            @Param("status") SaleStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
