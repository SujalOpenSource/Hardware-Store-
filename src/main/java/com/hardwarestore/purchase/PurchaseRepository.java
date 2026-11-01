
        package com.hardwarestore.purchase;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    boolean existsByPurchaseNumber(String purchaseNumber);

    @EntityGraph(attributePaths = {"supplier"})
    List<Purchase> findAllByOrderByPurchaseDateDesc();

    @EntityGraph(attributePaths = {"supplier"})
    Optional<Purchase> findWithSupplierById(Long id);

    @EntityGraph(attributePaths = {"supplier"})
    @Query("""
        SELECT p
        FROM Purchase p
        WHERE (:startDate IS NULL OR p.purchaseDate >= :startDate)
          AND (:endDate IS NULL OR p.purchaseDate <= :endDate)
          AND (:supplierId IS NULL OR p.supplier.id = :supplierId)
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.purchaseDate DESC
        """)
    List<Purchase> findPurchasesForReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("supplierId") Long supplierId,
            @Param("status") PurchaseStatus status
    );
}
