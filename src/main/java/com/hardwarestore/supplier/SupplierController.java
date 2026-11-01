
package com.hardwarestore.supplier;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierProductService supplierProductService;

    public SupplierController(
            SupplierService supplierService,
            SupplierProductService supplierProductService) {

        this.supplierService = supplierService;
        this.supplierProductService = supplierProductService;
    }

    @GetMapping
    public String listSuppliers(
            @RequestParam(required = false) String search,
            Model model) {

        model.addAttribute(
                "suppliers",
                supplierService.searchSuppliers(search)
        );

        model.addAttribute("search", search);

        return "supplier/list";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {

        model.addAttribute("supplier", new Supplier());
        model.addAttribute("pageTitle", "Add New Supplier");
        model.addAttribute("formAction", "/suppliers/save");

        return "supplier/form";
    }

    @PostMapping("/save")
    public String saveSupplier(
            @Valid @ModelAttribute("supplier") Supplier supplier,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Add New Supplier");
            model.addAttribute("formAction", "/suppliers/save");
            return "supplier/form";
        }

        try {
            supplierService.saveSupplier(supplier);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Supplier added successfully."
            );

            return "redirect:/suppliers";

        } catch (IllegalArgumentException exception) {

            model.addAttribute("pageTitle", "Add New Supplier");
            model.addAttribute("formAction", "/suppliers/save");
            model.addAttribute("errorMessage", exception.getMessage());

            return "supplier/form";
        }
    }

    @GetMapping("/{id}")
    public String viewSupplier(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "supplier",
                supplierService.findSupplierById(id)
        );

        model.addAttribute(
                "supplierProducts",
                supplierProductService.findSupplierProducts(id)
        );

        return "supplier/details";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "supplier",
                supplierService.findSupplierById(id)
        );

        model.addAttribute("pageTitle", "Edit Supplier");
        model.addAttribute("formAction", "/suppliers/update/" + id);

        return "supplier/form";
    }

    @PostMapping("/update/{id}")
    public String updateSupplier(
            @PathVariable Long id,
            @Valid @ModelAttribute("supplier") Supplier supplier,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Supplier");
            model.addAttribute("formAction", "/suppliers/update/" + id);
            return "supplier/form";
        }

        try {
            supplierService.updateSupplier(id, supplier);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Supplier updated successfully."
            );

            return "redirect:/suppliers";

        } catch (IllegalArgumentException exception) {

            model.addAttribute("pageTitle", "Edit Supplier");
            model.addAttribute("formAction", "/suppliers/update/" + id);
            model.addAttribute("errorMessage", exception.getMessage());

            return "supplier/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteSupplier(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            supplierService.deleteSupplier(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Supplier deleted successfully."
            );

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/suppliers";
    }
}
