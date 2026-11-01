
        package com.hardwarestore.purchase;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseReportData(
        Summary summary,
        List<SupplierRow> supplierRows,
        List<ProductRow> productRows,
        List<Purchase> purchases
) {

    public record Summary(
            long totalPurchases,
            long pendingPurchases,
            long receivedPurchases,
            long cancelledPurchases,
            BigDecimal totalAmount
    ) {}

    public record SupplierRow(
            String supplierName,
            long purchaseCount,
            BigDecimal totalAmount
    ) {}

    public record ProductRow(
            String productName,
            String sku,
            BigDecimal totalQuantity,
            BigDecimal totalAmount
    ) {}
}
