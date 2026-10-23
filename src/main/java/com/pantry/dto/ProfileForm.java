package com.pantry.dto;

import com.pantry.model.Allergen;
import com.pantry.model.Diet;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.EnumSet;
import java.util.Set;

public class ProfileForm {

    @NotBlank(message = "Inserisci la tua email")
    @Email(message = "Inserisci un indirizzo email valido")
    @Size(max = 120, message = "Massimo 120 caratteri")
    private String email;

    @NotNull(message = "Scegli il tuo regime alimentare")
    private Diet diet = Diet.ONNIVORA;

    private Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);

    @Size(max = 500, message = "Massimo 500 caratteri")
    private String foodPreferences;

    public String getEmail() {
        return email;
    }

    public Diet getDiet() {
        return diet;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public String getFoodPreferences() {
        return foodPreferences;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDiet(Diet diet) {
        this.diet = diet;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens = allergens;
    }

    public void setFoodPreferences(String foodPreferences) {
        this.foodPreferences = foodPreferences;
    }
}
