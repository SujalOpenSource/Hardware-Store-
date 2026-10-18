package com.hardwarestore.report;

import com.hardwarestore.sales.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    List<Sale> findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            String status, LocalDateTime from, LocalDateTime to);
}
