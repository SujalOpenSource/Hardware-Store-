package com.hardwarestore.product;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll(Sort sort);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);
}