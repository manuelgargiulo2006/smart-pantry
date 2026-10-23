package com.pantry.model;

import jakarta.persistence.*;

import java.util.EnumSet;
import java.util.Set;

/** Prodotto del catalogo generale, gestito dall'amministratore. */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(unique = true, length = 32)
    private String barcode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Durata media di conservazione in giorni: serve a proporre la data di scadenza. */
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_allergens", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "allergen", length = 30)
    private Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);

    public Product(String name, String barcode, Category category, Integer shelfLifeDays, Set<Allergen> allergens) {
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.shelfLifeDays = shelfLifeDays;
        this.allergens = allergens.isEmpty() ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(allergens);
    }

    public Product() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens = allergens;
    }
}
