package com.hardwarestore.sales;

import com.hardwarestore.customer.Customer;
import com.hardwarestore.customer.CustomerService;
import com.hardwarestore.product.Product;
import com.hardwarestore.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class SalesService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerService customerService;
    private final EntityManager entityManager;

    public SalesService(SaleRepository saleRepository,
                        ProductRepository productRepository,
                        CustomerService customerService,
                        EntityManager entityManager) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerService = customerService;
        this.entityManager = entityManager;
    }

    @Transactional
    public Sale createSale(SaleForm form) {
        if (form.getCustomerId() == null) {
            throw new IllegalArgumentException("Select a customer.");
        }
        Customer customer = customerService.getCustomerById(form.getCustomerId());

        Sale sale = new Sale();
        sale.setCustomer(customer);
        String invoice = "INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        sale.setInvoiceNumber(invoice);

        BigDecimal total = ZERO;
        int validLines = 0;

        if (form.getItems() == null) throw new IllegalArgumentException("Add at least one product.");

        for (SaleForm.SaleLineForm line : form.getItems()) {
            if (line == null || line.getProductId() == null) continue;
            BigDecimal quantity = line.getQuantity();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for every selected product.");
            }
            if (quantity.scale() > 3 && quantity.stripTrailingZeros().scale() > 3) {
                throw new IllegalArgumentException("Quantity can have at most three decimal places.");
            }

            // Lock product row so two simultaneous sales cannot oversell the same stock.
            Product product = entityManager.find(Product.class, line.getProductId(),
                    jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (product == null || !product.isActive()) {
                throw new IllegalArgumentException("A selected product does not exist or is inactive.");
            }
            if (product.getSellingPrice() == null || product.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Product has an invalid selling price: " + product.getName());
            }
            BigDecimal stock = product.getStockQuantity();
            if (stock == null || stock.compareTo(quantity) < 0) {
                throw new IllegalArgumentException("Insufficient stock for " + product.getName()
                        + ". Available: " + (stock == null ? "0" : stock.toPlainString()));
            }

            BigDecimal unitPrice = product.getSellingPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity.setScale(3, RoundingMode.HALF_UP));
            item.setSubtotal(subtotal);
            sale.addItem(item);

            product.setStockQuantity(stock.subtract(quantity).setScale(3, RoundingMode.HALF_UP));
            total = total.add(subtotal);
            validLines++;
        }

        if (validLines == 0) throw new IllegalArgumentException("Add at least one product to the sale.");

        sale.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        sale.setStatus("COMPLETED");
        return saleRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return saleRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Sale getSale(Long id) {
        return saleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
    }
}
