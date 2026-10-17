package com.hardwarestore.product;

import com.hardwarestore.category.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_sku",
                        columnNames = "sku"
                )
        },
        indexes = {
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_category", columnList = "category_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(
            name = "purchase_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(
            name = "selling_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(
            name = "stock_quantity",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(
            name = "minimum_stock",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}