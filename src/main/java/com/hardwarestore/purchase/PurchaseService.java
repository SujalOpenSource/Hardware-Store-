
        package com.hardwarestore.purchase;

import com.hardwarestore.inventory.InventoryService;
import com.hardwarestore.inventory.StockMovementType;
import com.hardwarestore.product.Product;
import com.hardwarestore.product.ProductRepository;
import com.hardwarestore.supplier.Supplier;
import com.hardwarestore.supplier.SupplierProduct;
import com.hardwarestore.supplier.SupplierProductRepository;
import com.hardwarestore.supplier.SupplierRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final InventoryService inventoryService;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            SupplierProductRepository supplierProductRepository,
            InventoryService inventoryService) {

        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.supplierProductRepository = supplierProductRepository;
        this.inventoryService = inventoryService;
    }

    public List<Purchase> findAllPurchases() {
        return purchaseRepository.findAllByOrderByPurchaseDateDesc();
    }

    public Purchase findPurchase(Long id) {
        return purchaseRepository.findWithSupplierById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase not found."));
    }

    public List<PurchaseItem> findPurchaseItems(Long purchaseId) {
        return purchaseItemRepository.findByPurchaseId(purchaseId);
    }

    @Transactional
    public Purchase createPurchase(
            Long supplierId,
            Map<Long, BigDecimal> productQuantities,
            String notes) {

        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select at least one product.");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Supplier not found."));

        if (!supplier.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot create a purchase for an inactive supplier.");
        }

        List<SupplierProduct> suppliedProducts =
                supplierProductRepository.findBySupplierId(supplierId);

        Map<Long, SupplierProduct> supplierProductMap = new HashMap<>();

        for (SupplierProduct supplierProduct : suppliedProducts) {
            supplierProductMap.put(
                    supplierProduct.getProduct().getId(),
                    supplierProduct
            );
        }

        Purchase purchase = new Purchase();
        purchase.setPurchaseNumber(generatePurchaseNumber());
        purchase.setSupplier(supplier);
        purchase.setStatus(PurchaseStatus.PENDING);
        purchase.setNotes(notes == null || notes.isBlank()
                ? null : notes.trim());

        BigDecimal totalAmount = BigDecimal.ZERO;

        Purchase savedPurchase = purchaseRepository.save(purchase);

        for (Map.Entry<Long, BigDecimal> entry
                : productQuantities.entrySet()) {

            Long productId = entry.getKey();
            BigDecimal quantity = entry.getValue();

            if (productId == null || quantity == null
                    || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Product quantity must be greater than zero.");
            }

            SupplierProduct supplierProduct =
                    supplierProductMap.get(productId);

            if (supplierProduct == null) {
                throw new IllegalArgumentException(
                        "One or more selected products are not supplied by this supplier.");
            }

            Product product = productRepository
                    .findWithCategoryById(productId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found: " + productId));

            if (!product.isActive()) {
                throw new IllegalArgumentException(
                        "Inactive products cannot be purchased.");
            }

            BigDecimal unitPrice = supplierProduct.getSupplierPrice();

            if (unitPrice == null
                    || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Invalid supplier purchase price for "
                                + product.getName());
            }

            BigDecimal subtotal = unitPrice.multiply(quantity);

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(savedPurchase);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);

            purchaseItemRepository.save(item);

            totalAmount = totalAmount.add(subtotal);
        }

        savedPurchase.setTotalAmount(totalAmount);

        return purchaseRepository.save(savedPurchase);
    }

    @Transactional
    public void receivePurchase(Long purchaseId) {

        Purchase purchase = purchaseRepository.findWithSupplierById(purchaseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase not found."));

        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending purchases can be received.");
        }

        List<PurchaseItem> items =
                purchaseItemRepository.findByPurchaseId(purchaseId);

        if (items.isEmpty()) {
            throw new IllegalStateException(
                    "This purchase has no products.");
        }

        for (PurchaseItem item : items) {
            inventoryService.adjustStock(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    StockMovementType.STOCK_IN,
                    "Purchase received: " + purchase.getPurchaseNumber()
            );
        }

        purchase.setStatus(PurchaseStatus.RECEIVED);
        purchaseRepository.save(purchase);
    }

    @Transactional
    public void cancelPurchase(Long purchaseId) {

        Purchase purchase = purchaseRepository.findWithSupplierById(purchaseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase not found."));

        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending purchases can be cancelled.");
        }

        purchase.setStatus(PurchaseStatus.CANCELLED);
        purchaseRepository.save(purchase);
    }

    public PurchaseReportData generateReport(
            LocalDate startDate,
            LocalDate endDate,
            Long supplierId,
            PurchaseStatus status) {

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate == null
                ? null : startDate.atStartOfDay();

        LocalDateTime endDateTime = endDate == null
                ? null : endDate.atTime(LocalTime.MAX);

        List<Purchase> purchases =
                purchaseRepository.findPurchasesForReport(
                        startDateTime,
                        endDateTime,
                        supplierId,
                        status
                );

        long pendingCount = 0;
        long receivedCount = 0;
        long cancelledCount = 0;

        BigDecimal totalAmount = BigDecimal.ZERO;

        Map<String, PurchaseReportData.SupplierRow> supplierTotals =
                new LinkedHashMap<>();

        Map<Long, ProductTotals> productTotals =
                new LinkedHashMap<>();

        for (Purchase purchase : purchases) {

            totalAmount = totalAmount.add(purchase.getTotalAmount());

            switch (purchase.getStatus()) {
                case PENDING -> pendingCount++;
                case RECEIVED -> receivedCount++;
                case CANCELLED -> cancelledCount++;
            }

            String supplierName = purchase.getSupplier().getCompanyName();

            PurchaseReportData.SupplierRow existingSupplier =
                    supplierTotals.get(supplierName);

            if (existingSupplier == null) {
                supplierTotals.put(
                        supplierName,
                        new PurchaseReportData.SupplierRow(
                                supplierName,
                                1,
                                purchase.getTotalAmount()
                        )
                );
            } else {
                supplierTotals.put(
                        supplierName,
                        new PurchaseReportData.SupplierRow(
                                supplierName,
                                existingSupplier.purchaseCount() + 1,
                                existingSupplier.totalAmount()
                                        .add(purchase.getTotalAmount())
                        )
                );
            }

            List<PurchaseItem> items =
                    purchaseItemRepository.findByPurchaseId(purchase.getId());

            for (PurchaseItem item : items) {

                Long productId = item.getProduct().getId();

                ProductTotals totals = productTotals.computeIfAbsent(
                        productId,
                        id -> new ProductTotals(
                                item.getProduct().getName(),
                                item.getProduct().getSku()
                        )
                );

                totals.quantity = totals.quantity.add(item.getQuantity());
                totals.amount = totals.amount.add(item.getSubtotal());
            }
        }

        List<PurchaseReportData.ProductRow> productRows =
                productTotals.values()
                        .stream()
                        .map(ProductTotals::toReportRow)
                        .sorted(Comparator.comparing(
                                PurchaseReportData.ProductRow::productName))
                        .toList();

        List<PurchaseReportData.SupplierRow> supplierRows =
                supplierTotals.values()
                        .stream()
                        .sorted(Comparator.comparing(
                                PurchaseReportData.SupplierRow::totalAmount,
                                Comparator.reverseOrder()))
                        .toList();

        PurchaseReportData.Summary summary =
                new PurchaseReportData.Summary(
                        purchases.size(),
                        pendingCount,
                        receivedCount,
                        cancelledCount,
                        totalAmount
                );

        return new PurchaseReportData(
                summary,
                supplierRows,
                productRows,
                purchases
        );
    }

    private static class ProductTotals {

        private final String productName;
        private final String sku;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal amount = BigDecimal.ZERO;

        private ProductTotals(String productName, String sku) {
            this.productName = productName;
            this.sku = sku;
        }

        private PurchaseReportData.ProductRow toReportRow() {
            return new PurchaseReportData.ProductRow(
                    productName,
                    sku,
                    quantity,
                    amount
            );
        }
    }

    private String generatePurchaseNumber() {
        return "PUR-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
