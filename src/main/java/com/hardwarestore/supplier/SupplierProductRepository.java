
package com.hardwarestore.supplier;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierProductRepository
        extends JpaRepository<SupplierProduct, Long> {

    boolean existsBySupplierIdAndProductId(
            Long supplierId,
            Long productId
    );

    @EntityGraph(attributePaths = {
            "supplier",
            "product",
            "product.category"
    })
    List<SupplierProduct> findBySupplierId(Long supplierId);

    @EntityGraph(attributePaths = {
            "supplier",
            "product",
            "product.category"
    })
    List<SupplierProduct> findByProductId(Long productId);

    @EntityGraph(attributePaths = {
            "supplier",
            "product"
    })
    List<SupplierProduct> findByProductIdIn(List<Long> productIds);

    @EntityGraph(attributePaths = {
            "supplier",
            "product",
            "product.category"
    })
    Optional<SupplierProduct> findByIdAndSupplierId(
            Long id,
            Long supplierId
    );

    @EntityGraph(attributePaths = {
            "supplier",
            "product"
    })
    List<SupplierProduct> findByProductIdAndPrimarySupplierTrue(
            Long productId
    );
}
