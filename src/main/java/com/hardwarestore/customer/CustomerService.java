
package com.hardwarestore.customer;

import org.springframework.stereotype.Service;
import java.util.List;

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
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found: " + id));
    }

    // Delete a customer
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
}
