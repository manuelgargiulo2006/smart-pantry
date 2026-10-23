package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.dto.AiRecipe;
import com.pantry.service.AiUnavailableException;
import com.pantry.service.RecipeAiService;
import com.pantry.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoint REST per il generatore di ricette, utilizzabile anche da Postman.
 * Esempio: POST http://localhost:8080/api/ai/recipe
 *          Autenticazione Basic (es. demo / demo1234)
 *          Body JSON: ["Zucchine", "Uova", "Panna da cucina"]
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final RecipeAiService recipeAiService;
    private final UserService userService;

    @PostMapping("/recipe")
    public ResponseEntity<?> getRecipeFromIngredients(@AuthenticationPrincipal PantryUserDetails principal,
                                                      @RequestBody(required = false) List<String> ingredients) {
        List<String> valid = ingredients == null ? List.of() : ingredients.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
        if (valid.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Per favore, inserisci almeno un ingrediente valido."));
        }
        try {
            AiRecipe recipe = recipeAiService.generateAntiWasteRecipe(valid, List.of(),
                    userService.getUser(principal.getId()));
            return ResponseEntity.ok(recipe);
        } catch (AiUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    public AiController(RecipeAiService recipeAiService, UserService userService) {
        this.recipeAiService = recipeAiService;
        this.userService = userService;
    }
}
