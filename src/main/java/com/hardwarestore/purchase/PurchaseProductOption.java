package com.hardwarestore.purchase;

import java.math.BigDecimal;

public record PurchaseProductOption(
        Long id,
        String name,
        String sku,
        String categoryName,
        BigDecimal supplierPrice
) {
}