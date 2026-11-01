package com.hardwarestore.sales;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SaleItemRequest {

    private Long productId;

    private BigDecimal quantity;
}