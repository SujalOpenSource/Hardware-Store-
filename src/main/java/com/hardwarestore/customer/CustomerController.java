
package com.hardwarestore.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // Display and search customers
    @GetMapping("/customers")
    public String listCustomers(
            @RequestParam(required = false) String keyword,
            Model model) {

        model.addAttribute(
                "customers",
                customerService.searchCustomers(keyword)
        );

        model.addAttribute("keyword", keyword);

        return "customer/list";
    }

    // Display the registration form
    @GetMapping("/customers/new")
    public String showCustomerForm(Model model) {

        model.addAttribute("customer", new Customer());
        model.addAttribute("pageTitle", "Add Customer");

        return "customer/form";
    }

    // Display the edit form
    @GetMapping("/customers/edit/{id}")
    public String showEditCustomerForm(
            @PathVariable Long id,
            Model model) {

        Customer customer = customerService.getCustomerById(id);

        model.addAttribute("customer", customer);
        model.addAttribute("pageTitle", "Edit Customer");

        return "customer/form";
    }

    // Save a new customer or update an existing customer
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
