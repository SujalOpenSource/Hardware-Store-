package com.hardwarestore.customer;

import com.hardwarestore.customer.Customer;
import com.hardwarestore.customer.CustomerService;
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
    public String listCustomers(Model model) {
        model.addAttribute(
                "customers",
                customerService.getAllCustomers()
        );
        return "customer/list";
    }

    // Display the registration form
    @GetMapping("/customers/new")
    public String showCustomerForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer/form";
    }

    // Save a new customer
    @PostMapping("/customers/save")
    public String saveCustomer(
            @ModelAttribute("customer") Customer customer) {
        customerService.saveCustomer(customer);
        return "redirect:/customers";
    }

    // Delete a customer
    @PostMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/customers";
    }
}
