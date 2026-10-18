package com.hardwarestore.report;

import com.hardwarestore.sales.Sale;
import com.hardwarestore.sales.SaleItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public ReportData getReport(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();
        if (toDate.isBefore(fromDate)) throw new IllegalArgumentException("End date cannot be before start date.");

        List<Sale> sales = reportRepository
                .findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        "COMPLETED", fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        BigDecimal revenue = sales.stream().map(Sale::getTotalAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byDay = new TreeMap<>();
        Map<String, BigDecimal> byProduct = new HashMap<>();
        for (Sale sale : sales) {
            String day = sale.getCreatedAt().toLocalDate().toString();
            byDay.merge(day, sale.getTotalAmount(), BigDecimal::add);
            for (SaleItem item : sale.getItems()) {
                byProduct.merge(item.getProductName(), item.getQuantity(), BigDecimal::add);
            }
        }
        List<ProductSummary> bestSellers = byProduct.entrySet().stream()
                .map(e -> new ProductSummary(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ProductSummary::quantity).reversed())
                .limit(10).collect(Collectors.toList());

        return new ReportData(fromDate, toDate, sales.size(), revenue, byDay, bestSellers, sales);
    }

    public record ProductSummary(String productName, BigDecimal quantity) {}
    public record ReportData(LocalDate fromDate, LocalDate toDate, int saleCount,
                             BigDecimal revenue, Map<String, BigDecimal> revenueByDay,
                             List<ProductSummary> bestSellers, List<Sale> sales) {}
}
