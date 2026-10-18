package com.hardwarestore.sales;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    List<Sale> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<Sale> findWithDetailsById(Long id);

    List<Sale> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);
}
