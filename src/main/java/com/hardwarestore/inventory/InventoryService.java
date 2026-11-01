
package com.hardwarestore.inventory;

import com.hardwarestore.product.Product;
import com.hardwarestore.product.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAllWithCategory(
                Sort.by(Sort.Direction.ASC, "name")
        );
    }

    @Transactional(readOnly = true)
    public List<Product> findLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    @Transactional(readOnly = true)
    public List<StockMovement> findRecentMovements() {
        return stockMovementRepository
                .findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<StockMovement> findProductMovements(Long productId) {
        return stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional
    public StockMovement adjustStock(
            Long productId,
            BigDecimal quantity,
            StockMovementType movementType,
            String reason) {

        if (productId == null) {
            throw new IllegalArgumentException(
                    "Please select a product."
            );
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        if (movementType == null) {
            throw new IllegalArgumentException(
                    "Please select a stock movement type."
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Please provide a reason for the stock adjustment."
            );
        }

        String cleanReason = reason.trim();

        if (cleanReason.length() > 500) {
            throw new IllegalArgumentException(
                    "Reason cannot exceed 500 characters."
            );
        }

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found with ID: " + productId
                        )
                );

        BigDecimal previousStock = product.getStockQuantity();
        BigDecimal newStock;

        if (movementType == StockMovementType.STOCK_IN) {
            newStock = previousStock.add(quantity);
        } else {
            if (previousStock.compareTo(quantity) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available quantity: "
                                + previousStock.toPlainString()
                );
            }

            newStock = previousStock.subtract(quantity);
        }

        product.setStockQuantity(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setReason(cleanReason);

        return stockMovementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public Product findProduct(Long productId) {
        return productRepository.findWithCategoryById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found with ID: " + productId
                        )
                );
    }
}
