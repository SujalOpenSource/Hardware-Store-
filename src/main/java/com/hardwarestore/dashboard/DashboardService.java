
package com.hardwarestore.dashboard;

import com.hardwarestore.product.Product;
import com.hardwarestore.product.ProductRepository;
import com.hardwarestore.purchase.Purchase;
import com.hardwarestore.purchase.PurchaseRepository;
import com.hardwarestore.purchase.PurchaseStatus;
import com.hardwarestore.sales.Sale;
import com.hardwarestore.sales.SaleItem;
import com.hardwarestore.sales.SaleItemRepository;
import com.hardwarestore.sales.SaleRepository;
import com.hardwarestore.sales.SaleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleItemRepository saleItemRepository;

    public DashboardService(
            ProductRepository productRepository,
            SaleRepository saleRepository,
            PurchaseRepository purchaseRepository,
            SaleItemRepository saleItemRepository
    ) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleItemRepository = saleItemRepository;
    }

    public DashboardData getDashboardData() {

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        List<Product> products = productRepository.findAll();
        List<Product> lowStockProducts =
                productRepository.findLowStockProducts();

        List<Sale> sales = saleRepository.findAllByOrderBySaleDateDesc();
        List<Purchase> purchases =
                purchaseRepository.findAllByOrderByPurchaseDateDesc();

        List<Sale> completedSales = sales.stream()
                .filter(sale -> sale.getStatus() == SaleStatus.COMPLETED)
                .toList();

        List<Sale> todaySales = completedSales.stream()
                .filter(sale ->
                        !sale.getSaleDate().isBefore(todayStart)
                                && sale.getSaleDate().isBefore(tomorrowStart))
                .toList();

        BigDecimal todayRevenue = todaySales.stream()
                .map(Sale::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal todayPurchaseTotal = purchases.stream()
                .filter(purchase ->
                        purchase.getStatus() == PurchaseStatus.RECEIVED)
                .filter(purchase ->
                        !purchase.getPurchaseDate().isBefore(todayStart)
                                && purchase.getPurchaseDate().isBefore(tomorrowStart))
                .map(Purchase::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);

        List<WeeklySales> weeklySales =
                getWeeklySales(completedSales, today);

        List<TopProduct> topProducts =
                getTopProducts(today);

        long activeProductCount = products.stream()
                .filter(Product::isActive)
                .count();

        return new DashboardData(
                activeProductCount,
                lowStockProducts.size(),
                todaySales.size(),
                todayRevenue,
                todayPurchaseTotal,
                weeklySales,
                topProducts
        );
    }

    private List<WeeklySales> getWeeklySales(
            List<Sale> completedSales,
            LocalDate today
    ) {
        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {
            revenueByDate.put(today.minusDays(i), ZERO);
        }

        for (Sale sale : completedSales) {
            LocalDate saleDate = sale.getSaleDate().toLocalDate();

            if (revenueByDate.containsKey(saleDate)) {
                revenueByDate.merge(
                        saleDate,
                        sale.getTotalAmount(),
                        BigDecimal::add
                );
            }
        }

        return revenueByDate.entrySet().stream()
                .map(entry -> new WeeklySales(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    private List<TopProduct> getTopProducts(LocalDate today) {

        LocalDateTime startDate = today.minusDays(29).atStartOfDay();
        LocalDateTime endDate = today.plusDays(1).atStartOfDay();

        List<SaleItem> items = saleItemRepository.findItemsForDashboard(
                SaleStatus.COMPLETED,
                startDate,
                endDate
        );

        Map<Long, ProductSalesSummary> productTotals = new HashMap<>();

        for (SaleItem item : items) {
            Product product = item.getProduct();

            productTotals.computeIfAbsent(
                    product.getId(),
                    id -> new ProductSalesSummary(
                            product.getName(),
                            product.getSku()
                    )
            ).add(item.getQuantity(), item.getSubtotal());
        }

        return productTotals.values().stream()
                .sorted(Comparator.comparing(
                        ProductSalesSummary::getQuantitySold,
                        Comparator.reverseOrder()
                ))
                .limit(5)
                .map(summary -> new TopProduct(
                        summary.getProductName(),
                        summary.getSku(),
                        summary.getQuantitySold(),
                        summary.getRevenue()
                ))
                .toList();
    }

    public record DashboardData(
            long activeProducts,
            long lowStockProducts,
            long todaySalesCount,
            BigDecimal todayRevenue,
            BigDecimal todayPurchaseTotal,
            List<WeeklySales> weeklySales,
            List<TopProduct> topProducts
    ) {
    }

    public record WeeklySales(
            LocalDate date,
            BigDecimal revenue
    ) {
    }

    public record TopProduct(
            String productName,
            String sku,
            BigDecimal quantitySold,
            BigDecimal revenue
    ) {
    }

    private static class ProductSalesSummary {

        private final String productName;
        private final String sku;
        private BigDecimal quantitySold = ZERO;
        private BigDecimal revenue = ZERO;

        private ProductSalesSummary(String productName, String sku) {
            this.productName = productName;
            this.sku = sku;
        }

        private void add(BigDecimal quantity, BigDecimal subtotal) {
            if (quantity != null) {
                quantitySold = quantitySold.add(quantity);
            }

            if (subtotal != null) {
                revenue = revenue.add(subtotal);
            }
        }

        private String getProductName() {
            return productName;
        }

        private String getSku() {
            return sku;
        }

        private BigDecimal getQuantitySold() {
            return quantitySold;
        }

        private BigDecimal getRevenue() {
            return revenue;
        }
    }
}
