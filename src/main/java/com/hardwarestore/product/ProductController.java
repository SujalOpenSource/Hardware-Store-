
package com.hardwarestore.product;

import com.hardwarestore.category.Category;
import com.hardwarestore.category.CategoryService;
import com.hardwarestore.supplier.SupplierProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductImageService productImageService;
    private final SupplierProductService supplierProductService;

    public ProductController(
            ProductService productService,
            CategoryService categoryService,
            ProductImageService productImageService,
            SupplierProductService supplierProductService) {

        this.productService = productService;
        this.categoryService = categoryService;
        this.productImageService = productImageService;
        this.supplierProductService = supplierProductService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            Model model) {

        List<Product> products =
                productService.searchProducts(search, categoryId);

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        Map<Long, String> supplierNames =
                supplierProductService.findSupplierNamesByProductIds(productIds);

        model.addAttribute("products", products);
        model.addAttribute("supplierNames", supplierNames);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("selectedCategoryId", categoryId);

        return "product/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {

        Product product = new Product();
        product.setCategory(new Category());

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageTitle", "Add Product");

        return "product/form";
    }

    @PostMapping("/save")
    public String save(
            @ModelAttribute("product") Product product,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "imageFile", required = false)
            MultipartFile imageFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        String newImagePath = null;
        String oldImagePath = null;

        try {
            if (product.getId() != null) {
                oldImagePath = productService
                        .findById(product.getId())
                        .getImagePath();
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                newImagePath = productImageService.saveImage(imageFile);
                product.setImagePath(newImagePath);
            }

            productService.save(product, categoryId);

            if (newImagePath != null
                    && oldImagePath != null
                    && !oldImagePath.equals(newImagePath)) {

                try {
                    productImageService.deleteImage(oldImagePath);
                } catch (IOException ignored) {
                    // Product was saved successfully.
                }
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product saved successfully."
            );

            return "redirect:/products";

        } catch (IllegalArgumentException | IOException exception) {

            if (newImagePath != null) {
                try {
                    productImageService.deleteImage(newImagePath);
                } catch (IOException ignored) {
                    // Preserve the original error.
                }
            }

            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute(
                    "pageTitle",
                    product.getId() == null ? "Add Product" : "Edit Product"
            );
            model.addAttribute("errorMessage", exception.getMessage());

            if (product.getCategory() == null) {
                product.setCategory(new Category());
            }

            product.getCategory().setId(categoryId);

            return "product/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Product product = productService.findById(id);

            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("pageTitle", "Edit Product");

            return "product/form";

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/products";
        }
    }

    @GetMapping("/details/{id}")
    public String details(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute("product", productService.findById(id));
            return "product/details";

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/products";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            Product product = productService.findById(id);
            String imagePath = product.getImagePath();

            productService.delete(id);

            if (imagePath != null) {
                try {
                    productImageService.deleteImage(imagePath);
                } catch (IOException ignored) {
                    // Product deletion succeeded.
                }
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product deleted successfully."
            );

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/products";
    }
}
