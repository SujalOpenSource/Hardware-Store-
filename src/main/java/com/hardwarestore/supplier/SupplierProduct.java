
package com.hardwarestore.supplier;

import com.hardwarestore.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "supplier_products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_supplier_product",
                        columnNames = {"supplier_id", "product_id"}
                )
        },
        indexes = {
                @Index(name = "idx_supplier_product_supplier", columnList = "supplier_id"),
                @Index(name = "idx_supplier_product_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "supplier_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal supplierPrice;

    @Column(name = "supplier_product_code", length = 100)
    private String supplierProductCode;

    @Column(name = "primary_supplier", nullable = false)
    private boolean primarySupplier = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
