package com.hardwarestore.product;

import com.hardwarestore.category.Category;
import com.hardwarestore.category.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll(
                Sort.by(Sort.Direction.ASC, "name")
        );
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found with ID: " + id
                        )
                );
    }

    public Product save(Product product, Long categoryId) {

        if (product.getName() == null ||
                product.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required."
            );
        }

        String name = product.getName().trim();

        if (name.length() > 150) {
            throw new IllegalArgumentException(
                    "Product name cannot exceed 150 characters."
            );
        }

        if (product.getSku() == null ||
                product.getSku().isBlank()) {
            throw new IllegalArgumentException("SKU is required.");
        }

        String sku = product.getSku().trim();

        if (sku.length() > 50) {
            throw new IllegalArgumentException(
                    "SKU cannot exceed 50 characters."
            );
        }

        boolean duplicateSku = product.getId() == null
                ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(
                sku, product.getId()
        );

        if (duplicateSku) {
            throw new IllegalArgumentException(
                    "This SKU is already assigned to another product."
            );
        }

        if (categoryId == null) {
            throw new IllegalArgumentException(
                    "Please select a category."
            );
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected category does not exist."
                        )
                );

        if (!category.isActive()) {
            throw new IllegalArgumentException(
                    "Please select an active category."
            );
        }

        requireNonNegative(
                product.getPurchasePrice(), "Purchase price"
        );

        requireNonNegative(
                product.getSellingPrice(), "Selling price"
        );

        requireNonNegative(
                product.getStockQuantity(), "Stock quantity"
        );

        requireNonNegative(
                product.getMinimumStock(), "Minimum stock"
        );

        // Preserve existing information when editing.
        if (product.getId() != null) {
            Product existing = findById(product.getId());

            product.setCreatedAt(existing.getCreatedAt());

            if (product.getImagePath() == null ||
                    product.getImagePath().isBlank()) {
                product.setImagePath(existing.getImagePath());
            }
        }

        product.setName(name);
        product.setSku(sku);
        product.setCategory(category);

        if (product.getDescription() != null) {
            product.setDescription(
                    product.getDescription().trim()
            );
        }

        return productRepository.save(product);
    }

    private void requireNonNegative(
            BigDecimal value, String fieldName) {

        if (value == null ||
                value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be zero or greater."
            );
        }
    }

    public void updateImagePath(Long productId, String imagePath) {
        Product product = findById(productId);
        product.setImagePath(imagePath);
        productRepository.save(product);
    }

    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }
}