
package com.hardwarestore.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportData(
        LocalDate startDate,
        LocalDate endDate,
        long totalSales,
        BigDecimal totalRevenue,
        BigDecimal averageSaleValue,
        List<ProductSalesRow> productSales,
        List<CustomerSalesRow> customerSales
) {
    public record ProductSalesRow(
            String productName,
            String sku,
            BigDecimal quantitySold,
            BigDecimal revenue
    ) {}

    public record CustomerSalesRow(
            String customerName,
            String customerPhone,
            long numberOfSales,
            BigDecimal totalSpent
    ) {}
}
