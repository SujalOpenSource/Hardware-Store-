
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

    // Display the edit customer form
    @GetMapping("/customers/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.getCustomerById(id);

        if (customer == null) {
            return "redirect:/customers";
        }

        model.addAttribute("customer", customer);
        return "customer/form";
    }

    // Update customer information
    @PostMapping("/customers/update/{id}")
    public String updateCustomer(
            @PathVariable Long id,
            @ModelAttribute("customer") Customer customer) {
        customer.setId(id);
        customerService.saveCustomer(customer);
        return "redirect:/customers";
    }

// Search customers
    @GetMapping("/customers/search")
    public String searchCustomers(
            @org.springframework.web.bind.annotation.RequestParam(
                    required = false) String keyword,
            Model model) {

        model.addAttribute(
                "customers",
                customerService.searchCustomers(keyword)
        );

        model.addAttribute("keyword", keyword);

        return "customer/list";
    }


}
