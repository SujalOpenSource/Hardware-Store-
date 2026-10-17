package com.hardwarestore.customer;
import jakarta.persistence.Column;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name="customers")
public class Customer {

    @Column(nullable = false)
    private boolean deleted = false;
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    @NotBlank(message = "Customer name is required.")
    @Size(max = 50,message = "Name cannot exceed 30 characters.")
    @Column(nullable=false,length =50)
    private String fullname;

    @NotBlank(message = "Email is required." )
    @Email(message = "Please enter your valid email address.")
    @Column(nullable = false,unique = true)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number cannot exceed 20 characters")
    @Column(nullable = false, length = 15)
    private String phone;

    @Size(max=50,message = "Address cannot exceed 50 characters.")
    @Column(length = 50)
    private String address;

    public Customer() {

    }
    public Customer(String fullname, String email, String phone, String address) {

        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.address = address;

    }


    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getFullname() {
        return fullname;
    }
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

}
