package com.hardwarestore.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;




@Controller

public class CustomerController {
    private final CustomerService customerService;
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    // Display all customers
    @GetMapping("/customers")
    public String listCustomers(Model model){
        model.addAttribute("customers",customerService.getAllCustomers()
        );
        return "customers/list";
    }
    // Display the  customer registration form
    @GetMapping("/customer/new")
    public String ShowCustomerForm(Model model){
        model.addAttribute("customer",new Customer());
        return "customer/form";
    }
    //Save a new customer
    @PostMapping("/customer/save")
    public String saveCustomer( @ ModelAttribute("customer")Customer customer ){
        customerService.saveCustomer(customer);
        return "redirect:/customers";
    }
    //Delete a customer
    @PostMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id){
        customerService.deleteCustomerById(id);
        return "redirect:/customers";
    }
}
