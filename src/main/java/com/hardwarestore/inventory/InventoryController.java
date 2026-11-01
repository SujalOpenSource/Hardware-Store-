
        package com.hardwarestore.inventory;

import com.hardwarestore.inventory.StockMovementType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute(
                "products",
                inventoryService.findAllProducts()
        );
        model.addAttribute(
                "lowStockProducts",
                inventoryService.findLowStockProducts()
        );
        model.addAttribute(
                "movements",
                inventoryService.findRecentMovements()
        );
        model.addAttribute("lowStockOnly", false);

        return "inventory/list";
    }

    @GetMapping("/low-stock")
    public String lowStock(Model model) {
        model.addAttribute(
                "products",
                inventoryService.findLowStockProducts()
        );
        model.addAttribute(
                "lowStockProducts",
                inventoryService.findLowStockProducts()
        );
        model.addAttribute(
                "movements",
                inventoryService.findRecentMovements()
        );
        model.addAttribute("lowStockOnly", true);

        return "inventory/list";
    }

    // Open the adjustment form without selecting a product.
    @GetMapping("/adjust")
    public String adjustForm(Model model) {
        prepareAdjustmentForm(model);
        return "inventory/adjust";
    }

    // Open the adjustment form with the clicked product selected.
    @GetMapping("/adjust/{id}")
    public String showAdjustmentForm(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Verify that the product exists.
            inventoryService.findProduct(id);

            prepareAdjustmentForm(model);
            model.addAttribute("selectedProductId", id);

            return "inventory/adjust";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/inventory";
        }
    }

    // Save the stock adjustment.
    @PostMapping("/adjust")
    public String adjustStock(
            @RequestParam Long productId,
            @RequestParam BigDecimal quantity,
            @RequestParam StockMovementType movementType,
            @RequestParam String reason,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            inventoryService.adjustStock(
                    productId,
                    quantity,
                    movementType,
                    reason
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Stock updated successfully and movement recorded."
            );

            return "redirect:/inventory";

        } catch (IllegalArgumentException exception) {
            prepareAdjustmentForm(model);

            model.addAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
            model.addAttribute("selectedProductId", productId);
            model.addAttribute("enteredQuantity", quantity);
            model.addAttribute("selectedMovementType", movementType);
            model.addAttribute("enteredReason", reason);

            return "inventory/adjust";
        }
    }

    // View the stock movement history for a specific product.
    @GetMapping("/history/{productId}")
    public String productHistory(
            @PathVariable Long productId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute(
                    "product",
                    inventoryService.findProduct(productId)
            );
            model.addAttribute(
                    "movements",
                    inventoryService.findProductMovements(productId)
            );

            return "inventory/history";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/inventory";
        }
    }

    private void prepareAdjustmentForm(Model model) {
        model.addAttribute(
                "products",
                inventoryService.findAllProducts()
        );
        model.addAttribute(
                "movementTypes",
                StockMovementType.values()
        );
    }
}
