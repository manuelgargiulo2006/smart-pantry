package com.pantry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ShoppingItemForm {

    @NotNull(message = "Scegli un prodotto")
    private Long productId;

    @NotNull(message = "Indica la quantità")
    @Min(value = 1, message = "Almeno 1")
    @Max(value = 999, message = "Al massimo 999")
    private Integer quantity = 1;

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
