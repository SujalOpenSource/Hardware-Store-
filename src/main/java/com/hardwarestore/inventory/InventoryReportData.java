
package com.hardwarestore.inventory;

import com.hardwarestore.product.Product;

import java.math.BigDecimal;
import java.util.List;

public record InventoryReportData(
        Summary summary,
        List<Product> products,
        List<Product> lowStockProducts,
        List<StockMovement> movements,
        List<ProductRow> productRows
) {
    public record Summary(
            long totalProducts,
            BigDecimal totalUnits,
            BigDecimal totalStockValue,
            long lowStockCount,
            long outOfStockCount,
            BigDecimal stockInQuantity,
            BigDecimal stockOutQuantity
    ) {}

    public record ProductRow(
            String productName,
            String sku,
            BigDecimal stockIn,
            BigDecimal stockOut
    ) {}
}
