
package com.hardwarestore.report;

import com.hardwarestore.sales.Sale;
import com.hardwarestore.sales.SaleItem;
import com.hardwarestore.sales.SaleItemRepository;
import com.hardwarestore.sales.SaleRepository;
import com.hardwarestore.sales.SaleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    public ReportService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository
    ) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @Transactional(readOnly = true)
    public SalesReportData generateSalesReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Please select a valid date range.");
        }

        List<Sale> completedSales = saleRepository
                .findAllByOrderBySaleDateDesc()
                .stream()
                .filter(sale -> sale.getStatus() == SaleStatus.COMPLETED)
                .filter(sale -> {
                    LocalDate saleDate = sale.getSaleDate().toLocalDate();
                    return !saleDate.isBefore(startDate)
                            && !saleDate.isAfter(endDate);
                })
                .toList();

        BigDecimal totalRevenue = ZERO;
        Map<Long, ProductAccumulator> products = new LinkedHashMap<>();
        Map<String, CustomerAccumulator> customers = new LinkedHashMap<>();

        for (Sale sale : completedSales) {
            totalRevenue = totalRevenue.add(sale.getTotalAmount());

            String customerKey = sale.getCustomer() != null
                    ? "CUSTOMER-" + sale.getCustomer().getId()
                    : "WALK-IN-" + sale.getCustomerName()
                    + "-" + sale.getCustomerPhone();

            CustomerAccumulator customer = customers.computeIfAbsent(
                    customerKey,
                    key -> new CustomerAccumulator(
                            sale.getCustomerName(),
                            sale.getCustomerPhone()
                    )
            );

            customer.numberOfSales++;
            customer.totalSpent = customer.totalSpent.add(sale.getTotalAmount());

            List<SaleItem> items = saleItemRepository
                    .findBySaleIdOrderByIdAsc(sale.getId());

            for (SaleItem item : items) {
                Long productId = item.getProduct().getId();

                ProductAccumulator product = products.computeIfAbsent(
                        productId,
                        key -> new ProductAccumulator(
                                item.getProduct().getName(),
                                item.getProduct().getSku()
                        )
                );

                product.quantitySold =
                        product.quantitySold.add(item.getQuantity());

                product.revenue =
                        product.revenue.add(item.getSubtotal());
            }
        }

        BigDecimal averageSaleValue = completedSales.isEmpty()
                ? ZERO
                : totalRevenue.divide(
                BigDecimal.valueOf(completedSales.size()),
                2,
                RoundingMode.HALF_UP
        );

        List<SalesReportData.ProductSalesRow> productRows =
                products.values().stream()
                        .map(product -> new SalesReportData.ProductSalesRow(
                                product.name,
                                product.sku,
                                product.quantitySold,
                                product.revenue
                        ))
                        .sorted(Comparator.comparing(
                                SalesReportData.ProductSalesRow::revenue
                        ).reversed())
                        .toList();

        List<SalesReportData.CustomerSalesRow> customerRows =
                customers.values().stream()
                        .map(customer -> new SalesReportData.CustomerSalesRow(
                                customer.name,
                                customer.phone,
                                customer.numberOfSales,
                                customer.totalSpent
                        ))
                        .sorted(Comparator.comparing(
                                SalesReportData.CustomerSalesRow::totalSpent
                        ).reversed())
                        .toList();

        return new SalesReportData(
                startDate,
                endDate,
                completedSales.size(),
                totalRevenue,
                averageSaleValue,
                productRows,
                customerRows
        );
    }

    private static class ProductAccumulator {
        private final String name;
        private final String sku;
        private BigDecimal quantitySold = BigDecimal.ZERO;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProductAccumulator(String name, String sku) {
            this.name = name;
            this.sku = sku;
        }
    }

    private static class CustomerAccumulator {
        private final String name;
        private final String phone;
        private long numberOfSales;
        private BigDecimal totalSpent = BigDecimal.ZERO;

        private CustomerAccumulator(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }
}
