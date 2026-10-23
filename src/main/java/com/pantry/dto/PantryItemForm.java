package com.pantry.dto;

import com.pantry.model.PantryItem;
import com.pantry.model.Storage;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Dati del form per inserire o modificare un prodotto in dispensa. */
public class PantryItemForm {

    @NotNull(message = "Scegli un prodotto dal catalogo")
    private Long productId;

    @NotNull(message = "Indica la quantità")
    @Min(value = 1, message = "Almeno 1")
    @Max(value = 999, message = "Al massimo 999")
    private Integer quantity = 1;

    @NotNull(message = "Indica la data di acquisto")
    @PastOrPresent(message = "La data di acquisto non può essere nel futuro")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate purchaseDate = LocalDate.now();

    @NotNull(message = "Indica la data di scadenza")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expirationDate;

    @NotNull(message = "Indica dove lo conservi")
    private Storage storage = Storage.DISPENSA;

    /** Facoltativa: sotto questa quantità il prodotto va nella lista della spesa. */
    @Min(value = 1, message = "Almeno 1")
    @Max(value = 999, message = "Al massimo 999")
    private Integer minQuantity;

    public static PantryItemForm from(PantryItem item, Integer minQuantity) {
        PantryItemForm form = new PantryItemForm();
        form.setProductId(item.getProduct().getId());
        form.setQuantity(item.getQuantity());
        form.setPurchaseDate(item.getPurchaseDate());
        form.setExpirationDate(item.getExpirationDate());
        form.setStorage(item.getStorage());
        form.setMinQuantity(minQuantity);
        return form;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public Storage getStorage() {
        return storage;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }
}
