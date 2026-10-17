
package com.hardwarestore.category;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute(
                "categories",
                categoryService.findAll()
        );

        return "categories/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("pageTitle", "Add Category");

        return "categories/form";
    }

    @PostMapping("/save")
    public String save(
            @ModelAttribute("category") Category category,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            categoryService.save(category);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Category saved successfully."
            );

            return "redirect:/categories";

        } catch (IllegalArgumentException exception) {
            model.addAttribute("pageTitle",
                    category.getId() == null
                            ? "Add Category"
                            : "Edit Category");

            model.addAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "categories/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute(
                    "category",
                    categoryService.findById(id)
            );

            model.addAttribute("pageTitle", "Edit Category");

            return "categories/form";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/categories";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            categoryService.delete(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Category deleted successfully."
            );

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/categories";
    }
}
