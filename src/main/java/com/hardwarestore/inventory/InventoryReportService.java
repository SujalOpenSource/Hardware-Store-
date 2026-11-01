
package com.hardwarestore.inventory;

import com.hardwarestore.product.Product;
import com.hardwarestore.product.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryReportService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryReportService(
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public InventoryReportData generateReport(
            LocalDate startDate,
            LocalDate endDate) {

        validateDates(startDate, endDate);

        LocalDateTime startDateTime = startDate == null
                ? null : startDate.atStartOfDay();

        LocalDateTime endDateTime = endDate == null
                ? null : endDate.atTime(LocalTime.MAX);

        List<Product> products = productRepository.findAllWithCategory(
                Sort.by(Sort.Direction.ASC, "name")
        );

        List<Product> lowStockProducts = products.stream()
                .filter(product -> product.getStockQuantity()
                        .compareTo(product.getMinimumStock()) <= 0)
                .toList();

        List<StockMovement> movements =
                stockMovementRepository.findMovementsForReport(
                        startDateTime,
                        endDateTime
                );

        BigDecimal totalUnits = BigDecimal.ZERO;
        BigDecimal totalStockValue = BigDecimal.ZERO;
        long outOfStockCount = 0;

        for (Product product : products) {
            BigDecimal quantity = product.getStockQuantity();
            BigDecimal purchasePrice = product.getPurchasePrice();

            totalUnits = totalUnits.add(quantity);
            totalStockValue = totalStockValue.add(
                    purchasePrice.multiply(quantity)
            );

            if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                outOfStockCount++;
            }
        }

        BigDecimal stockInQuantity = movements.stream()
                .filter(m -> m.getMovementType()
                        == StockMovementType.STOCK_IN)
                .map(StockMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal stockOutQuantity = movements.stream()
                .filter(m -> m.getMovementType()
                        == StockMovementType.STOCK_OUT)
                .map(StockMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, List<StockMovement>> movementsByProduct =
                movements.stream().collect(
                        Collectors.groupingBy(
                                movement -> movement.getProduct().getId()
                        )
                );

        List<InventoryReportData.ProductRow> productRows =
                products.stream()
                        .map(product -> {
                            List<StockMovement> productMovements =
                                    movementsByProduct.getOrDefault(
                                            product.getId(),
                                            List.of()
                                    );

                            BigDecimal stockIn = productMovements.stream()
                                    .filter(m -> m.getMovementType()
                                            == StockMovementType.STOCK_IN)
                                    .map(StockMovement::getQuantity)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            BigDecimal stockOut = productMovements.stream()
                                    .filter(m -> m.getMovementType()
                                            == StockMovementType.STOCK_OUT)
                                    .map(StockMovement::getQuantity)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            return new InventoryReportData.ProductRow(
                                    product.getName(),
                                    product.getSku(),
                                    stockIn,
                                    stockOut
                            );
                        })
                        .toList();

        InventoryReportData.Summary summary =
                new InventoryReportData.Summary(
                        products.size(),
                        totalUnits,
                        totalStockValue,
                        lowStockProducts.size(),
                        outOfStockCount,
                        stockInQuantity,
                        stockOutQuantity
                );

        return new InventoryReportData(
                summary,
                products,
                lowStockProducts,
                movements,
                productRows
        );
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date."
            );
        }
    }
}
