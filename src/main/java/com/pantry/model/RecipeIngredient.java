package com.pantry.model;

import jakarta.persistence.*;

@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    /** Collegamento al catalogo: può essere vuoto per ingredienti non presenti (es. "sale"). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(length = 50)
    private String amount;

    public RecipeIngredient(Product product, String ingredientName, String amount) {
        this.product = product;
        this.ingredientName = ingredientName;
        this.amount = amount;
    }

    public RecipeIngredient() {
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Product getProduct() {
        return product;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getAmount() {
        return amount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
