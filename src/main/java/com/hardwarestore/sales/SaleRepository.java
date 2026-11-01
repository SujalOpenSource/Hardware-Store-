package com.hardwarestore.sales;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = "customer")
    List<Sale> findAllByOrderBySaleDateDesc();

    @EntityGraph(attributePaths = "customer")
    Optional<Sale> findById(Long id);

    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);
}