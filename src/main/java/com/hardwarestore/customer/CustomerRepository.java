package com.hardwarestore.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
            String name, String phone, String email
    );

    List<Customer> findAllByOrderByNameAsc();
}