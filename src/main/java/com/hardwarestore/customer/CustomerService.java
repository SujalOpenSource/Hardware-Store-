package com.hardwarestore.customer;

import java.util.List;

import com.hardwarestore.customer.Customer;
import com.hardwarestore.customer.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Register or update a customer
    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Get all customers
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // Get a customer by ID
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    // Delete a customer by ID
    public void deleteCustomerById(Long id) {
        customerRepository.deleteById(id);
    }

    // Keep this method if your existing controller uses it
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    // Search customers by name, phone, or email
    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return customerRepository.findAll();
        }

        String search = keyword.trim();

        List<Customer> customers =
                customerRepository.findByFullnameContainingIgnoreCase(search);

        customers.addAll(
                customerRepository.findByPhoneContaining(search)
        );

        customers.addAll(
                customerRepository.findByEmailContainingIgnoreCase(search)
        );


        return customers.stream().distinct().toList();
    }

}