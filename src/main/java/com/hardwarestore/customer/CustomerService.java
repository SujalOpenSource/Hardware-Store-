
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
        return customerRepository.findByDeletedFalse();
    }

    // Get a customer by ID
    public Customer getCustomerById(Long id) {
        return customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found: " + id));
    }



    // Search by name, phone, or email
    public List<Customer> searchCustomers(String keyword) {
        return customerRepository.searchActiveCustomers(keyword);
    }


    // Delete a customer
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customer.setDeleted(true);
        customerRepository.save(customer);
    }
}
