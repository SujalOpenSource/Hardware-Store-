package com.hardwarestore.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFullnameContainingIgnoreCase(String fullname);

    List<Customer> findByPhoneContaining(String phone);

    List<Customer> findByEmailContainingIgnoreCase(String email);
    List<Customer> findByAddressContainingIgnoreCase(String address);

}

