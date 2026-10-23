package com.pantry.service;

import com.pantry.dto.AiRecipe;
import com.pantry.model.Allergen;
import com.pantry.model.User;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generatore di ricette anti-spreco con Spring AI.
 * La risposta dell'AI viene convertita direttamente nell'oggetto Java {@link AiRecipe}
 * (Structured Output con BeanOutputConverter) invece che in testo semplice.
 */
@Service
public class RecipeAiService {

    private final ChatClient chatClient;

    /** Versione base: solo la lista degli ingredienti in scadenza. */
    public AiRecipe generateAntiWasteRecipe(List<String> ingredients) {
        return generateAntiWasteRecipe(ingredients, List.of(), null);
    }

    /**
     * Versione completa: tiene conto anche degli altri prodotti in dispensa,
     * delle allergie e del regime alimentare indicati nel profilo.
     */
    public AiRecipe generateAntiWasteRecipe(List<String> ingredients, List<String> otherAvailable, User user) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new BusinessException("Seleziona almeno un ingrediente");
        }
        String ingredientsList = String.join(", ", ingredients);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Agisci come uno chef esperto di cucina svuota-frigo. ")
              .append("Crea una ricetta gustosa utilizzando principalmente questi ingredienti che stanno per scadere: ")
              .append(ingredientsList).append(". ")
              .append("Se necessario, puoi includere ingredienti base comunemente presenti in casa ")
              .append("(es. sale, olio, acqua, farina).\n");
        if (otherAvailable != null && !otherAvailable.isEmpty()) {
            prompt.append("In dispensa ci sono anche: ").append(String.join(", ", otherAvailable))
                  .append(". Puoi usarli se servono.\n");
        }
        if (user != null) {
            prompt.append("Regime alimentare da rispettare: ").append(user.getDiet().getLabel()).append(".\n");
            if (!user.getAllergens().isEmpty()) {
                String allergens = user.getAllergens().stream()
                        .map(Allergen::getLabel)
                        .collect(Collectors.joining(", "));
                prompt.append("ATTENZIONE: l'utente è allergico a: ").append(allergens)
                      .append(". La ricetta NON deve contenere questi allergeni.\n");
            }
            if (user.getFoodPreferences() != null) {
                prompt.append("Preferenze dell'utente: ").append(user.getFoodPreferences()).append(".\n");
            }
        }
        prompt.append("Nel campo ingredientiMancanti elenca solo gli ingredienti non presenti tra quelli indicati ")
              .append("e diversi dagli ingredienti base. ")
              .append("Scrivi nomi di ingredienti semplici (es. \"Zucchine\", non \"zucchine fresche tagliate\"). ")
              .append("Fornisci la risposta in italiano.");

        return AiErrors.call(() -> chatClient.prompt()
                .user(prompt.toString())
                .call()
                .entity(AiRecipe.class));
    }

    public RecipeAiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}
