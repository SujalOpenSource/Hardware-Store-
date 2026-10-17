
package com.hardwarestore.category;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll(
                Sort.by(Sort.Direction.ASC, "name")
        );
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Category not found with ID: " + id
                        ));
    }

    @Transactional(readOnly = true)
    public boolean nameExists(String name, Long id) {
        if (id == null) {
            return categoryRepository.existsByNameIgnoreCase(name);
        }

        return categoryRepository
                .existsByNameIgnoreCaseAndIdNot(name, id);
    }

    public void save(Category category) {
        category.setName(category.getName().trim());

        if (category.getDescription() != null) {
            category.setDescription(
                    category.getDescription().trim()
            );
        }

        if (category.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required."
            );
        }

        if (category.getName().length() > 100) {
            throw new IllegalArgumentException(
                    "Category name cannot exceed 100 characters."
            );
        }

        if (nameExists(category.getName(), category.getId())) {
            throw new IllegalArgumentException(
                    "A category with this name already exists."
            );
        }

        categoryRepository.save(category);
    }

    public void delete(Long id) {
        Category category = findById(id);
        categoryRepository.delete(category);
    }
}
