
package com.hardwarestore.supplier;

import com.hardwarestore.product.Product;
import com.hardwarestore.product.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SupplierProductService {


    @Transactional(readOnly = true)
    public SupplierProduct findSupplierProduct(
            Long supplierId,
            Long associationId) {

        return supplierProductRepository
                .findByIdAndSupplierId(associationId, supplierId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier product association not found."
                        )
                );
    }

    @Transactional
    public void updateSupplierProduct(
            Long supplierId,
            Long associationId,
            BigDecimal supplierPrice,
            String supplierProductCode,
            boolean primarySupplier) {

        if (supplierPrice == null ||
                supplierPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Supplier price cannot be negative."
            );
        }

        String cleanCode = supplierProductCode == null
                || supplierProductCode.isBlank()
                ? null
                : supplierProductCode.trim();

        if (cleanCode != null && cleanCode.length() > 100) {
            throw new IllegalArgumentException(
                    "Supplier product code cannot exceed 100 characters."
            );
        }

        SupplierProduct supplierProduct =
                supplierProductRepository
                        .findByIdAndSupplierId(associationId, supplierId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Supplier product association not found."
                                )
                        );

        if (primarySupplier) {
            removeExistingPrimarySupplier(
                    supplierProduct.getProduct().getId()
            );
        }

        supplierProduct.setSupplierPrice(supplierPrice);
        supplierProduct.setSupplierProductCode(cleanCode);
        supplierProduct.setPrimarySupplier(primarySupplier);

        supplierProductRepository.save(supplierProduct);
    }


    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SupplierProductRepository supplierProductRepository;

    public SupplierProductService(
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            SupplierProductRepository supplierProductRepository) {

        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.supplierProductRepository = supplierProductRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierProduct> findSupplierProducts(Long supplierId) {

        return supplierProductRepository.findBySupplierId(supplierId);
    }

    @Transactional(readOnly = true)
    public List<Product> findAvailableProducts() {

        return productRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "name")
                )
                .stream()
                .filter(Product::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findSupplierNamesByProductIds(
            List<Long> productIds) {

        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        return supplierProductRepository.findByProductIdIn(productIds)
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.mapping(
                                item -> item.getSupplier().getCompanyName(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        names -> String.join(", ", names)
                                )
                        )
                ));
    }

    @Transactional
    public SupplierProduct addProduct(
            Long supplierId,
            Long productId,
            BigDecimal supplierPrice,
            String supplierProductCode,
            boolean primarySupplier) {

        if (supplierId == null || productId == null) {
            throw new IllegalArgumentException(
                    "Please select a supplier and product."
            );
        }

        if (supplierPrice == null
                || supplierPrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Supplier price cannot be negative."
            );
        }

        if (supplierProductCode != null
                && supplierProductCode.trim().length() > 100) {

            throw new IllegalArgumentException(
                    "Supplier product code cannot exceed 100 characters."
            );
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Supplier not found.")
                );

        Product product = productRepository.findWithCategoryById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found.")
                );

        if (!supplier.isActive()) {
            throw new IllegalArgumentException(
                    "This supplier is inactive."
            );
        }

        if (!product.isActive()) {
            throw new IllegalArgumentException(
                    "This product is inactive and cannot be assigned."
            );
        }

        if (supplierProductRepository.existsBySupplierIdAndProductId(
                supplierId, productId)) {

            throw new IllegalArgumentException(
                    "This product is already assigned to this supplier."
            );
        }

        if (primarySupplier) {
            removeExistingPrimarySupplier(productId);
        }

        SupplierProduct supplierProduct = new SupplierProduct();

        supplierProduct.setSupplier(supplier);
        supplierProduct.setProduct(product);
        supplierProduct.setSupplierPrice(supplierPrice);
        supplierProduct.setSupplierProductCode(
                supplierProductCode == null
                        || supplierProductCode.isBlank()
                        ? null
                        : supplierProductCode.trim()
        );
        supplierProduct.setPrimarySupplier(primarySupplier);

        return supplierProductRepository.save(supplierProduct);
    }

    @Transactional
    public void removeProduct(Long supplierId, Long associationId) {

        SupplierProduct supplierProduct =
                supplierProductRepository.findByIdAndSupplierId(
                        associationId, supplierId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier product association not found."
                        )
                );

        supplierProductRepository.delete(supplierProduct);
    }

    @Transactional
    public void setPrimarySupplier(Long supplierId, Long associationId) {

        SupplierProduct selected =
                supplierProductRepository.findByIdAndSupplierId(
                        associationId, supplierId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier product association not found."
                        )
                );

        removeExistingPrimarySupplier(selected.getProduct().getId());

        selected.setPrimarySupplier(true);

        supplierProductRepository.save(selected);
    }

    private void removeExistingPrimarySupplier(Long productId) {

        List<SupplierProduct> existingPrimary =
                supplierProductRepository
                        .findByProductIdAndPrimarySupplierTrue(productId);

        for (SupplierProduct association : existingPrimary) {
            association.setPrimarySupplier(false);
        }

        supplierProductRepository.saveAll(existingPrimary);
    }
}
