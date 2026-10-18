package com.hardwarestore.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SaleForm {
    @NotNull(message = "Please select a customer.")
    private Long customerId;

    @Valid
    private List<SaleLineForm> items = new ArrayList<>();

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public List<SaleLineForm> getItems() { return items; }
    public void setItems(List<SaleLineForm> items) { this.items = items; }

    public static class SaleLineForm {
        private Long productId;
        private BigDecimal quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    }
}
