
package com.hardwarestore.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByCompanyNameIgnoreCase(String companyName);

    boolean existsByCompanyNameIgnoreCaseAndIdNot(
            String companyName,
            Long id
    );

    @Query("""
            SELECT s
            FROM Supplier s
            WHERE :search IS NULL
               OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY s.companyName ASC
            """)
    List<Supplier> searchSuppliers(
            @Param("search") String search
    );
}
