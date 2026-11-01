
package com.hardwarestore.supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Supplier> searchSuppliers(String search) {
        String keyword = search == null || search.isBlank()
                ? null
                : search.trim();

        return supplierRepository.searchSuppliers(keyword);
    }

    @Transactional(readOnly = true)
    public Supplier findSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found with ID: " + id
                        )
                );
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {

        normalizeSupplier(supplier);

        if (supplierRepository.existsByCompanyNameIgnoreCase(
                supplier.getCompanyName())) {
            throw new IllegalArgumentException(
                    "A supplier with this company name already exists."
            );
        }

        supplier.setActive(true);

        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier updateSupplier(Long id, Supplier updatedSupplier) {

        Supplier existingSupplier = findSupplierById(id);

        normalizeSupplier(updatedSupplier);

        if (supplierRepository.existsByCompanyNameIgnoreCaseAndIdNot(
                updatedSupplier.getCompanyName(), id)) {
            throw new IllegalArgumentException(
                    "Another supplier with this company name already exists."
            );
        }

        existingSupplier.setCompanyName(updatedSupplier.getCompanyName());
        existingSupplier.setContactPerson(updatedSupplier.getContactPerson());
        existingSupplier.setPhone(updatedSupplier.getPhone());
        existingSupplier.setEmail(updatedSupplier.getEmail());
        existingSupplier.setAddress(updatedSupplier.getAddress());
        existingSupplier.setNotes(updatedSupplier.getNotes());

        return supplierRepository.save(existingSupplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {

        Supplier supplier = findSupplierById(id);

        supplierRepository.delete(supplier);
    }

    private void normalizeSupplier(Supplier supplier) {

        supplier.setCompanyName(supplier.getCompanyName().trim());
        supplier.setPhone(supplier.getPhone().trim());

        supplier.setContactPerson(cleanOptional(supplier.getContactPerson()));
        supplier.setEmail(cleanOptional(supplier.getEmail()));
        supplier.setAddress(cleanOptional(supplier.getAddress()));
        supplier.setNotes(cleanOptional(supplier.getNotes()));
    }

    private String cleanOptional(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
