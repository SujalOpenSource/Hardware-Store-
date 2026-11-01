
package com.hardwarestore.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    boolean existsByBillNumber(String billNumber);

    Optional<Bill> findByBillNumber(String billNumber);

    boolean existsBySaleId(Long saleId);

    @EntityGraph(attributePaths = {"sale", "sale.customer"})
    List<Bill> findAllByOrderByBillDateDesc();

    @EntityGraph(attributePaths = {"sale", "sale.customer"})
    @Query("SELECT b FROM Bill b WHERE b.id = :id")
    Optional<Bill> findBillWithSaleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bill b WHERE b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);
}
