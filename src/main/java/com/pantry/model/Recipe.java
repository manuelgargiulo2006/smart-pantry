package com.pantry.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    /** Passaggi di preparazione, uno per riga. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "prep_minutes", nullable = false)
    private Integer prepMinutes;

    @Column(nullable = false)
    private Integer servings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Diet diet = Diet.ONNIVORA;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    public void addIngredient(RecipeIngredient ingredient) {
        ingredient.setRecipe(this);
        ingredients.add(ingredient);
    }

    /** Passaggi come lista, per mostrarli numerati nella pagina. */
    public List<String> getSteps() {
        return Arrays.stream(instructions.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Recipe() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public Integer getPrepMinutes() {
        return prepMinutes;
    }

    public Integer getServings() {
        return servings;
    }

    public Diet getDiet() {
        return diet;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setPrepMinutes(Integer prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public void setDiet(Diet diet) {
        this.diet = diet;
    }

    public void setAiGenerated(boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }
}
