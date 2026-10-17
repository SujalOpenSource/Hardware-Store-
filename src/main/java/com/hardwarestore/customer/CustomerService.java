package com.hardwarestore.customer;

import org.springframework.stereotype.Service;

import java.util.List;

@Service


public class CustomerService {
    private final  CustomerRepository customerRepository;
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    //Register or Update a customer.
    public Customer saveCustomer(Customer  customer){
        return customerRepository.save(customer);
    }
    // Get all customer
     public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
     }
     // Get a customer by id
     public Customer getCustomerById(Long id){
        return customerRepository.findById(id).get();
     }
     // Delete a customer
     public void deleteCustomerById(Long id){
        customerRepository.deleteById(id);
     }


    public void deleteCustomer(Long id) {
    }
}
