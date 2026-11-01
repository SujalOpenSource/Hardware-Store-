
package com.hardwarestore.billing;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "bill")
    List<Payment> findByBillIdOrderByPaymentDateDesc(Long billId);

    List<Payment> findAllByOrderByPaymentDateDesc();

    long countByBillId(Long billId);
}
