package com.hardwarestore.sales;

import com.hardwarestore.customer.Customer;
import com.hardwarestore.product.Product;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SaleService saleService;
    private final SalesPdfService salesPdfService;

    @GetMapping
    public String listSales(Model model) {
        model.addAttribute("sales", saleService.findAllSales());
        return "sales/list";
    }

    @GetMapping("/new")
    public String showSaleForm(Model model) {
        loadFormData(model);
        return "sales/create";
    }

    @PostMapping
    public String createSale(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String walkInName,
            @RequestParam(required = false) String walkInPhone,
            @RequestParam(required = false) String walkInAddress,
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(required = false) List<BigDecimal> quantities,
            @RequestParam(required = false) String notes,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (productIds == null || quantities == null
                    || productIds.size() != quantities.size()) {
                throw new IllegalArgumentException(
                        "Please add valid products and quantities."
                );
            }

            List<SaleItemRequest> items = new ArrayList<>();

            for (int i = 0; i < productIds.size(); i++) {
                SaleItemRequest item = new SaleItemRequest();
                item.setProductId(productIds.get(i));
                item.setQuantity(quantities.get(i));
                items.add(item);
            }

            Sale sale = saleService.createSale(
                    customerId,
                    walkInName,
                    walkInPhone,
                    walkInAddress,
                    items,
                    notes
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Sale completed successfully. Invoice: " + sale.getInvoiceNumber()
            );

            return "redirect:/sales/" + sale.getId();

        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            loadFormData(model);
            return "sales/create";
        }
    }

    @GetMapping("/{id}")
    public String saleDetails(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute("sale", saleService.findSale(id));
            model.addAttribute("items", saleService.findSaleItems(id));
            return "sales/details";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "The requested sale could not be found."
            );
            return "redirect:/sales";
        }
    }

    @GetMapping("/{id}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long id
    ) {
        Sale sale = saleService.findSale(id);
        List<SaleItem> items = saleService.findSaleItems(id);

        byte[] pdf = salesPdfService.generateInvoice(sale, items);

        return org.springframework.http.ResponseEntity.ok()
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Invoice-" + sale.getInvoiceNumber() + ".pdf\""
                )
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void loadFormData(Model model) {
        List<Product> products = saleService.findAvailableProducts();
        List<Customer> customers = saleService.findCustomers();

        model.addAttribute("products", products);
        model.addAttribute("customers", customers);
    }
}