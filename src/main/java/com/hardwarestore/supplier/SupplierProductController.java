
package com.hardwarestore.supplier;

import com.hardwarestore.product.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/suppliers/{supplierId}/products")
public class SupplierProductController {

    private final SupplierService supplierService;
    private final SupplierProductService supplierProductService;

    public SupplierProductController(
            SupplierService supplierService,
            SupplierProductService supplierProductService) {

        this.supplierService = supplierService;
        this.supplierProductService = supplierProductService;
    }

    @GetMapping
    public String manageProducts(
            @PathVariable Long supplierId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            loadPage(supplierId, model);
            return "supplier/products";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/suppliers";
        }
    }

    @PostMapping("/add")
    public String addProduct(
            @PathVariable Long supplierId,
            @RequestParam Long productId,
            @RequestParam BigDecimal supplierPrice,
            @RequestParam(required = false) String supplierProductCode,
            @RequestParam(defaultValue = "false") boolean primarySupplier,
            RedirectAttributes redirectAttributes) {

        try {
            supplierProductService.addProduct(
                    supplierId,
                    productId,
                    supplierPrice,
                    supplierProductCode,
                    primarySupplier
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product successfully assigned to supplier."
            );

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToProducts(supplierId);
    }

    @GetMapping("/edit/{associationId}")
    public String editProductForm(
            @PathVariable Long supplierId,
            @PathVariable Long associationId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            SupplierProduct supplierProduct =
                    supplierProductService.findSupplierProduct(
                            supplierId,
                            associationId
                    );

            model.addAttribute(
                    "supplier",
                    supplierService.findSupplierById(supplierId)
            );

            model.addAttribute("supplierProduct", supplierProduct);

            return "supplier/product-edit";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return redirectToProducts(supplierId);
        }
    }

    @PostMapping("/edit/{associationId}")
    public String updateProduct(
            @PathVariable Long supplierId,
            @PathVariable Long associationId,
            @RequestParam BigDecimal supplierPrice,
            @RequestParam(required = false) String supplierProductCode,
            @RequestParam(defaultValue = "false") boolean primarySupplier,
            RedirectAttributes redirectAttributes) {

        try {
            supplierProductService.updateSupplierProduct(
                    supplierId,
                    associationId,
                    supplierPrice,
                    supplierProductCode,
                    primarySupplier
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Supplier product details updated successfully."
            );

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/suppliers/" + supplierId
                    + "/products/edit/" + associationId;
        }

        return redirectToProducts(supplierId);
    }

    @PostMapping("/remove/{associationId}")
    public String removeProduct(
            @PathVariable Long supplierId,
            @PathVariable Long associationId,
            RedirectAttributes redirectAttributes) {

        try {
            supplierProductService.removeProduct(
                    supplierId,
                    associationId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product removed from this supplier."
            );

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToProducts(supplierId);
    }

    @PostMapping("/primary/{associationId}")
    public String setPrimarySupplier(
            @PathVariable Long supplierId,
            @PathVariable Long associationId,
            RedirectAttributes redirectAttributes) {

        try {
            supplierProductService.setPrimarySupplier(
                    supplierId,
                    associationId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Primary supplier updated successfully."
            );

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToProducts(supplierId);
    }

    private void loadPage(Long supplierId, Model model) {

        model.addAttribute(
                "supplier",
                supplierService.findSupplierById(supplierId)
        );

        model.addAttribute(
                "supplierProducts",
                supplierProductService.findSupplierProducts(supplierId)
        );

        List<Product> availableProducts =
                supplierProductService.findAvailableProducts();

        model.addAttribute("availableProducts", availableProducts);
    }

    private String redirectToProducts(Long supplierId) {
        return "redirect:/suppliers/" + supplierId + "/products";
    }
}
