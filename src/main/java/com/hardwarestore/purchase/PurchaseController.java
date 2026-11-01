package com.hardwarestore.purchase;

import com.hardwarestore.product.ProductRepository;
import com.hardwarestore.supplier.Supplier;
import com.hardwarestore.supplier.SupplierProduct;
import com.hardwarestore.supplier.SupplierProductRepository;
import com.hardwarestore.supplier.SupplierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/purchases")
public class PurchaseController {

    private static final String QUANTITY_PREFIX = "quantity_";

    private final PurchaseService purchaseService;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SupplierProductRepository supplierProductRepository;

    public PurchaseController(PurchaseService purchaseService,
                              SupplierRepository supplierRepository,
                              ProductRepository productRepository,
                              SupplierProductRepository supplierProductRepository) {
        this.purchaseService = purchaseService;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.supplierProductRepository = supplierProductRepository;
    }

    @GetMapping
    public String listPurchases(Model model) {
        model.addAttribute("purchases", purchaseService.findAllPurchases());
        return "purchase/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("suppliers", supplierRepository.findAll());
        return "purchase/form";
    }

    @GetMapping(value = "/supplier/{supplierId}/products",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<PurchaseProductOption> getSupplierProducts(
            @PathVariable Long supplierId) {

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));

        if (!supplier.isActive()) {
            return List.of();
        }

        return supplierProductRepository.findBySupplierId(supplierId)
                .stream()
                .filter(item -> item.getProduct().isActive())
                .map(item -> {
                    var product = item.getProduct();

                    return new PurchaseProductOption(
                            product.getId(),
                            product.getName(),
                            product.getSku(),
                            product.getCategory().getName(),
                            item.getSupplierPrice()
                    );
                })
                .toList();
    }

    @PostMapping
    public String createPurchase(@RequestParam Long supplierId,
                                 @RequestParam Map<String, String> formData,
                                 RedirectAttributes redirectAttributes) {
        try {
            Map<Long, BigDecimal> productQuantities = new HashMap<>();

            for (Map.Entry<String, String> entry : formData.entrySet()) {
                String fieldName = entry.getKey();

                if (!fieldName.startsWith(QUANTITY_PREFIX)) {
                    continue;
                }

                String quantityValue = entry.getValue();

                if (quantityValue == null || quantityValue.isBlank()) {
                    continue;
                }

                Long productId = Long.parseLong(
                        fieldName.substring(QUANTITY_PREFIX.length())
                );

                BigDecimal quantity = new BigDecimal(quantityValue.trim());
                productQuantities.put(productId, quantity);
            }

            purchaseService.createPurchase(
                    supplierId,
                    productQuantities,
                    formData.get("notes")
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Purchase created successfully."
            );

            return "redirect:/purchases";

        } catch (NumberFormatException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter valid product quantities."
            );

            return "redirect:/purchases/new";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/purchases/new";
        }
    }

    @GetMapping("/{id}")
    public String viewPurchase(@PathVariable Long id, Model model) {
        Purchase purchase = purchaseService.findPurchase(id);

        model.addAttribute("purchase", purchase);
        model.addAttribute("items", purchaseService.findPurchaseItems(id));

        return "purchase/details";
    }

    @PostMapping("/{id}/receive")
    public String receivePurchase(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            purchaseService.receivePurchase(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Purchase received and inventory updated successfully."
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/purchases/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelPurchase(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            purchaseService.cancelPurchase(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Purchase cancelled successfully."
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/purchases/" + id;
    }
}