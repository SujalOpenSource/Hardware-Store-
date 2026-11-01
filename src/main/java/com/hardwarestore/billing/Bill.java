
package com.hardwarestore.billing;

import com.hardwarestore.sales.Sale;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bills",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bill_number",
                        columnNames = "bill_number"
                ),
                @UniqueConstraint(
                        name = "uk_bill_sale",
                        columnNames = "sale_id"
                )
        },
        indexes = {
                @Index(name = "idx_bill_date", columnList = "bill_date"),
                @Index(name = "idx_bill_status", columnList = "payment_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", nullable = false, unique = true, length = 40)
    private String billNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false, unique = true)
    private Sale sale;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private BillingStatus paymentStatus;

    @Column(name = "bill_date", nullable = false)
    private LocalDateTime billDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (billDate == null) {
            billDate = LocalDateTime.now();
        }
    }
}
