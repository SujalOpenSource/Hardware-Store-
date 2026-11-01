package com.hardwarestore.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllCustomers();
        }

        String search = keyword.trim();

        return customerRepository
                .findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
                        search, search, search
                );
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found with ID: " + id
                        ));
    }

    public Customer saveCustomer(Customer customer) {
        validateCustomer(customer);
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customerRepository.delete(customer);
    }

    private void validateCustomer(Customer customer) {
        if (customer.getName() == null
                || customer.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer name is required."
            );
        }

        if (customer.getPhone() == null
                || customer.getPhone().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer phone number is required."
            );
        }

        customer.setName(customer.getName().trim());
        customer.setPhone(customer.getPhone().trim());

        if (customer.getEmail() != null
                && customer.getEmail().isBlank()) {
            customer.setEmail(null);
        } else if (customer.getEmail() != null) {
            customer.setEmail(customer.getEmail().trim());
        }

        if (customer.getAddress() != null
                && customer.getAddress().isBlank()) {
            customer.setAddress(null);
        } else if (customer.getAddress() != null) {
            customer.setAddress(customer.getAddress().trim());
        }
    }
}