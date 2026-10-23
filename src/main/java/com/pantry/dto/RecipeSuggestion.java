package com.pantry.dto;

import com.pantry.model.Allergen;
import com.pantry.model.Recipe;

import java.util.List;
import java.util.Set;

/**
 * Ricetta del database confrontata con la dispensa dell'utente.
 *
 * @param expiringUsed ingredienti in scadenza che la ricetta permette di consumare
 * @param available    ingredienti della ricetta già presenti in dispensa
 * @param missing      ingredienti da comprare
 * @param allergens    allergeni della ricetta che l'utente ha dichiarato nel profilo
 */
public record RecipeSuggestion(Recipe recipe,
                               List<String> expiringUsed,
                               List<String> available,
                               List<String> missing,
                               Set<Allergen> allergens) {

    public boolean isSafe() {
        return allergens.isEmpty();
    }
}
