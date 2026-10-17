package com.hardwarestore.customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFullnameContainingIgnoreCase(String fullname);

    List<Customer> findByPhoneContaining(String phone);

    List<Customer> findByEmailContainingIgnoreCase(String email);

    List<Customer> findByAddressContainingIgnoreCase(String address);

    List<Customer> findByFullnameContainingIgnoreCaseOrPhoneContainingOrEmailContainingIgnoreCase(
            String fullname, String phone, String email);

    List<Customer> findByDeletedFalse();

    Optional<Customer> findByIdAndDeletedFalse(Long id);

    @Query("""
    SELECT c FROM Customer c
    WHERE  c.deleted = false
    AND (
    LOWER (c.fullname) LIKE LOWER(CONCAT('%', :keyword, '%') ) 
    OR c.phone LIKE CONCAT( '%', :keyword, '%')
    OR LOWER( c.email) LIKE LOWER( CONCAT( '%' , :keyword, '%') )   
    )
""")
    List<Customer> searchActiveCustomers(@Param("keyword") String keyword);
}

