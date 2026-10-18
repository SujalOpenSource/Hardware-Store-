package com.hardwarestore.sales;

import com.hardwarestore.customer.CustomerService;
import com.hardwarestore.product.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sales")
public class SalesController {
    private final SalesService salesService;
    private final CustomerService customerService;
    private final ProductRepository productRepository;

    public SalesController(SalesService salesService, CustomerService customerService,
                           ProductRepository productRepository) {
        this.salesService = salesService;
        this.customerService = customerService;
        this.productRepository = productRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("sales", salesService.getAllSales());
        return "sales/list";
    }

    @GetMapping("/new")
    public String newSale(Model model) {
        SaleForm form = new SaleForm();
        for (int i = 0; i < 5; i++) form.getItems().add(new SaleForm.SaleLineForm());
        model.addAttribute("saleForm", form);
        populateChoices(model);
        return "sales/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("saleForm") SaleForm form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateChoices(model);
            model.addAttribute("errorMessage", "Please select a customer and check the sale details.");
            return "sales/form";
        }
        try {
            Sale sale = salesService.createSale(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Sale recorded successfully: " + sale.getInvoiceNumber());
            return "redirect:/billing/" + sale.getId();
        } catch (IllegalArgumentException ex) {
            populateChoices(model);
            model.addAttribute("errorMessage", ex.getMessage());
            return "sales/form";
        }
    }

    private void populateChoices(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("products", productRepository.findAll());
    }
}
