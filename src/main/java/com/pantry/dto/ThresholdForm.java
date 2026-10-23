package com.pantry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ThresholdForm {

    @NotNull(message = "Scegli un prodotto")
    private Long productId;

    @NotNull(message = "Indica la soglia minima")
    @Min(value = 1, message = "Almeno 1")
    @Max(value = 999, message = "Al massimo 999")
    private Integer minQuantity = 1;

    public Long getProductId() {
        return productId;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }
}
