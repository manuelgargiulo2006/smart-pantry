package com.pantry.dto;

import com.pantry.model.Allergen;
import com.pantry.model.Product;
import jakarta.validation.constraints.*;

import java.util.EnumSet;
import java.util.Set;

/** Form dell'admin per creare o modificare un prodotto del catalogo. */
public class ProductForm {

    @NotBlank(message = "Inserisci il nome del prodotto")
    @Size(max = 100, message = "Massimo 100 caratteri")
    private String name;

    @Pattern(regexp = "^$|^[0-9]{8,14}$", message = "Il codice a barre deve avere da 8 a 14 cifre")
    private String barcode;

    @NotNull(message = "Scegli una categoria")
    private Long categoryId;

    @Min(value = 1, message = "Almeno 1 giorno")
    @Max(value = 3650, message = "Al massimo 3650 giorni")
    private Integer shelfLifeDays;

    private Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);

    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.setName(product.getName());
        form.setBarcode(product.getBarcode());
        form.setCategoryId(product.getCategory().getId());
        form.setShelfLifeDays(product.getShelfLifeDays());
        form.setAllergens(product.getAllergens().isEmpty()
                ? EnumSet.noneOf(Allergen.class)
                : EnumSet.copyOf(product.getAllergens()));
        return form;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens = allergens;
    }
}
