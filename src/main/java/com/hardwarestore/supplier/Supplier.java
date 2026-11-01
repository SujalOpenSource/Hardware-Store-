
package com.hardwarestore.supplier;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "suppliers",
        indexes = {
                @Index(name = "idx_supplier_company_name", columnList = "company_name"),
                @Index(name = "idx_supplier_phone", columnList = "phone")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Company name is required.")
    @Size(max = 150, message = "Company name cannot exceed 150 characters.")
    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Size(max = 100, message = "Contact person cannot exceed 100 characters.")
    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @NotBlank(message = "Phone number is required.")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters.")
    @Column(nullable = false, length = 20)
    private String phone;

    @Email(message = "Please enter a valid email address.")
    @Size(max = 120, message = "Email cannot exceed 120 characters.")
    @Column(length = 120)
    private String email;

    @Size(max = 500, message = "Address cannot exceed 500 characters.")
    @Column(length = 500)
    private String address;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters.")
    @Column(length = 1000)
    private String notes;

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
