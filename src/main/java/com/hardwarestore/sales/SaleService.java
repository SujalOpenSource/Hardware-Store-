
package com.hardwarestore.sales;

import com.hardwarestore.customer.Customer;
import com.hardwarestore.customer.CustomerRepository;
import com.hardwarestore.inventory.InventoryService;
import com.hardwarestore.inventory.StockMovementType;
import com.hardwarestore.product.Product;
import com.hardwarestore.product.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<Sale> findAllSales() {
        return saleRepository.findAllByOrderBySaleDateDesc();
    }

    @Transactional(readOnly = true)
    public Sale findSale(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Sale not found.")
                );
    }

    @Transactional(readOnly = true)
    public List<SaleItem> findSaleItems(Long saleId) {
        findSale(saleId);
        return saleItemRepository.findBySaleIdOrderByIdAsc(saleId);
    }

    @Transactional(readOnly = true)
    public List<Product> findAvailableProducts() {
        return productRepository.findAllWithCategory(
                        Sort.by(Sort.Direction.ASC, "name")
                )
                .stream()
                .filter(Product::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Customer> findCustomers() {
        return customerRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public Sale createSale(
            Long customerId,
            String walkInName,
            String walkInPhone,
            String walkInAddress,
            List<SaleItemRequest> requestedItems,
            String notes
    ) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Add at least one product to the sale."
            );
        }

        if (notes != null && notes.length() > 500) {
            throw new IllegalArgumentException(
                    "Notes cannot exceed 500 characters."
            );
        }

        Customer customer = null;
        String customerName;
        String customerPhone;
        String customerAddress;

        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Customer not found.")
                    );

            customerName = customer.getName();
            customerPhone = customer.getPhone();
            customerAddress = customer.getAddress();

        } else {
            customerName = requireText(
                    walkInName,
                    "Customer name is required."
            );

            customerPhone = requireText(
                    walkInPhone,
                    "Customer phone number is required."
            );

            customerAddress = requireText(
                    walkInAddress,
                    "Customer address is required."
            );

            if (customerName.length() > 150
                    || customerPhone.length() > 30
                    || customerAddress.length() > 500) {
                throw new IllegalArgumentException(
                        "Customer details exceed the allowed length."
                );
            }
        }

        Set<Long> selectedProductIds = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Sale sale = new Sale();
        sale.setInvoiceNumber(generateInvoiceNumber());
        sale.setCustomer(customer);
        sale.setCustomerName(customerName);
        sale.setCustomerPhone(customerPhone);
        sale.setCustomerAddress(customerAddress);
        sale.setSaleDate(LocalDateTime.now());
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setNotes(
                notes == null || notes.isBlank() ? null : notes.trim()
        );
        sale.setTotalAmount(BigDecimal.ZERO);

        sale = saleRepository.save(sale);

        for (SaleItemRequest request : requestedItems) {

            if (request == null || request.getProductId() == null) {
                throw new IllegalArgumentException(
                        "Select a valid product for every sale item."
                );
            }

            if (!selectedProductIds.add(request.getProductId())) {
                throw new IllegalArgumentException(
                        "A product can only appear once in a sale. "
                                + "Combine its quantity into one item."
                );
            }

            BigDecimal quantity = request.getQuantity();

            if (quantity == null
                    || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Product quantity must be greater than zero."
                );
            }

            Product product = productRepository
                    .findByIdForUpdate(request.getProductId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("Product not found.")
                    );

            if (!product.isActive()) {
                throw new IllegalArgumentException(
                        product.getName() + " is inactive."
                );
            }

            BigDecimal unitPrice = product.getSellingPrice();

            if (unitPrice == null
                    || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Invalid selling price for " + product.getName() + "."
                );
            }

            BigDecimal subtotal = unitPrice.multiply(quantity)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(quantity);
            saleItem.setUnitPrice(unitPrice);
            saleItem.setSubtotal(subtotal);

            saleItemRepository.save(saleItem);

            inventoryService.adjustStock(
                    product.getId(),
                    quantity,
                    StockMovementType.STOCK_OUT,
                    "Sale invoice " + sale.getInvoiceNumber()
            );

            totalAmount = totalAmount.add(subtotal);
        }

        sale.setTotalAmount(
                totalAmount.setScale(
                        2,
                        java.math.RoundingMode.HALF_UP
                )
        );

        return saleRepository.save(sale);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String generateInvoiceNumber() {
        String date = LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );

        String invoiceNumber;

        do {
            invoiceNumber = "INV-" + date + "-"
                    + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (saleRepository.existsByInvoiceNumber(invoiceNumber));

        return invoiceNumber;
    }
}
