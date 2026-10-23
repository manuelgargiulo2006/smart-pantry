package com.pantry.service;

import com.pantry.dto.AiRecipe;
import com.pantry.dto.RecipeSuggestion;
import com.pantry.model.*;
import com.pantry.repository.ProductRepository;
import com.pantry.repository.RecipeRepository;
import com.pantry.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Database di ricette e incrocio con la dispensa: suggerisce cosa cucinare
 * per consumare prima i prodotti che stanno per scadere.
 */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PantryService pantryService;

    @Transactional(readOnly = true)
    public List<Recipe> getAll() {
        return recipeRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Recipe getRecipe(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ricetta non trovata"));
    }

    /**
     * Ricette suggerite: solo quelle che usano almeno un prodotto in scadenza,
     * compatibili con il regime alimentare e senza allergeni dichiarati dall'utente.
     * Ordinate per numero di prodotti in scadenza usati e poi per ingredienti mancanti.
     */
    @Transactional(readOnly = true)
    public List<RecipeSuggestion> suggest(User user) {
        Set<Long> expiringIds = pantryService.getExpiringSoon(user.getId()).stream()
                .map(i -> i.getProduct().getId())
                .collect(Collectors.toSet());
        if (expiringIds.isEmpty()) {
            return List.of();
        }
        Set<Long> available = availableProductIds(user.getId());
        return getAll().stream()
                .filter(r -> user.getDiet().accepts(r.getDiet()))
                .map(r -> evaluate(r, user, expiringIds, available))
                .filter(s -> !s.expiringUsed().isEmpty() && s.isSafe())
                .sorted(Comparator.comparingInt((RecipeSuggestion s) -> s.expiringUsed().size()).reversed()
                        .thenComparingInt(s -> s.missing().size()))
                .toList();
    }

    /** Confronto di tutte le ricette con la dispensa, per la pagina "Ricettario". */
    @Transactional(readOnly = true)
    public List<RecipeSuggestion> evaluateAll(User user) {
        Set<Long> expiringIds = pantryService.getExpiringSoon(user.getId()).stream()
                .map(i -> i.getProduct().getId())
                .collect(Collectors.toSet());
        Set<Long> available = availableProductIds(user.getId());
        return getAll().stream()
                .map(r -> evaluate(r, user, expiringIds, available))
                .toList();
    }

    @Transactional(readOnly = true)
    public RecipeSuggestion evaluate(Recipe recipe, User user) {
        Set<Long> expiringIds = pantryService.getExpiringSoon(user.getId()).stream()
                .map(i -> i.getProduct().getId())
                .collect(Collectors.toSet());
        return evaluate(recipe, user, expiringIds, availableProductIds(user.getId()));
    }

    /** Prodotti presenti in dispensa e non scaduti. */
    private Set<Long> availableProductIds(Long userId) {
        return pantryService.getItems(userId).stream()
                .filter(i -> i.getStatus() != ExpiryStatus.SCADUTO)
                .map(i -> i.getProduct().getId())
                .collect(Collectors.toSet());
    }

    private RecipeSuggestion evaluate(Recipe recipe, User user, Set<Long> expiringIds, Set<Long> availableIds) {
        List<String> expiringUsed = new ArrayList<>();
        List<String> available = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            Product product = ingredient.getProduct();
            if (product == null) {
                // ingredienti di base non presenti nel catalogo (sale, acqua...): si danno per disponibili
                continue;
            }
            if (expiringIds.contains(product.getId())) {
                expiringUsed.add(product.getName());
            }
            if (availableIds.contains(product.getId())) {
                available.add(product.getName());
            } else {
                missing.add(product.getName());
            }
            product.getAllergens().stream()
                    .filter(user.getAllergens()::contains)
                    .forEach(allergens::add);
        }
        return new RecipeSuggestion(recipe, expiringUsed, available, missing, allergens);
    }

    /** Salva nel database una ricetta generata dall'AI, collegando gli ingredienti al catalogo. */
    @Transactional
    public Recipe saveAiRecipe(Long userId, AiRecipe ai) {
        String name = ai.nome() == null || ai.nome().isBlank() ? "Ricetta anti-spreco" : ai.nome().trim();
        if (recipeRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Nel ricettario esiste già una ricetta chiamata \"" + name + "\"");
        }
        Recipe recipe = new Recipe();
        recipe.setName(truncate(name, 150));
        recipe.setDescription(truncate(ai.descrizione(), 500));
        recipe.setInstructions(String.join("\n", ai.passaggi() == null ? List.of("Nessun passaggio indicato") : ai.passaggi()));
        recipe.setPrepMinutes(Math.max(1, ai.tempoPreparazione()));
        recipe.setServings(Math.max(1, ai.porzioni()));
        recipe.setDiet(parseDiet(ai.regime()));
        recipe.setAiGenerated(true);
        recipe.setCreatedBy(userRepository.getReferenceById(userId));
        if (ai.ingredienti() != null) {
            for (AiRecipe.Ingrediente ingredient : ai.ingredienti()) {
                if (ingredient.nome() == null || ingredient.nome().isBlank()) {
                    continue;
                }
                String ingredientName = ingredient.nome().trim();
                Product product = productRepository.findByNameIgnoreCase(ingredientName).orElse(null);
                recipe.addIngredient(new RecipeIngredient(product, truncate(ingredientName, 100),
                        truncate(ingredient.quantita(), 50)));
            }
        }
        return recipeRepository.save(recipe);
    }

    @Transactional
    public void delete(Long id) {
        recipeRepository.delete(getRecipe(id));
    }

    static Diet parseDiet(String value) {
        if (value == null) {
            return Diet.ONNIVORA;
        }
        try {
            return Diet.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Diet.ONNIVORA;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    public RecipeService(RecipeRepository recipeRepository, ProductRepository productRepository, UserRepository userRepository, PantryService pantryService) {
        this.recipeRepository = recipeRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.pantryService = pantryService;
    }
}
