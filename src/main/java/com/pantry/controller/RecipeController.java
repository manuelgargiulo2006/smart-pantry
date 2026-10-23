package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.dto.AiRecipe;
import com.pantry.dto.RecipeSuggestion;
import com.pantry.model.Diet;
import com.pantry.model.ExpiryStatus;
import com.pantry.model.PantryItem;
import com.pantry.model.Recipe;
import com.pantry.model.User;
import com.pantry.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/recipes")
public class RecipeController {

    private static final String SESSION_AI_RECIPE = "lastAiRecipe";

    private final RecipeService recipeService;
    private final RecipeAiService recipeAiService;
    private final PantryService pantryService;
    private final UserService userService;

    // ---------------------------------------------------------------- ricettario

    @GetMapping
    public String list(@AuthenticationPrincipal PantryUserDetails principal,
                       @RequestParam(required = false) Diet diet,
                       @RequestParam(required = false) String q,
                       Model model) {
        User user = userService.getUser(principal.getId());
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ITALIAN);
        List<RecipeSuggestion> all = recipeService.evaluateAll(user).stream()
                .filter(s -> diet == null || s.recipe().getDiet() == diet)
                .filter(s -> query.isEmpty() || s.recipe().getName().toLowerCase(Locale.ITALIAN).contains(query))
                .toList();
        model.addAttribute("suggestions", recipeService.suggest(user));
        model.addAttribute("expiring", pantryService.getExpiringSoon(user.getId()));
        model.addAttribute("recipes", all);
        model.addAttribute("diets", Diet.values());
        model.addAttribute("diet", diet);
        model.addAttribute("q", q);
        model.addAttribute("user", user);
        model.addAttribute("active", "recipes");
        return "recipes/list";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal PantryUserDetails principal, @PathVariable Long id, Model model) {
        User user = userService.getUser(principal.getId());
        Recipe recipe = recipeService.getRecipe(id);
        model.addAttribute("s", recipeService.evaluate(recipe, user));
        model.addAttribute("user", user);
        model.addAttribute("active", "recipes");
        return "recipes/detail";
    }

    // ---------------------------------------------------------------- generatore AI

    @GetMapping("/ai")
    public String aiForm(@AuthenticationPrincipal PantryUserDetails principal, HttpSession session, Model model) {
        prepareAiPage(principal.getId(), null, model);
        session.removeAttribute(SESSION_AI_RECIPE);
        return "recipes/ai";
    }

    @PostMapping("/ai")
    public String aiGenerate(@AuthenticationPrincipal PantryUserDetails principal,
                             @RequestParam(name = "ingredients", required = false) List<String> ingredients,
                             HttpSession session,
                             Model model) {
        User user = userService.getUser(principal.getId());
        List<String> selected = ingredients == null ? List.of() : ingredients.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
        prepareAiPage(user.getId(), selected, model);
        if (selected.isEmpty()) {
            model.addAttribute("error", "Seleziona almeno un ingrediente");
            return "recipes/ai";
        }
        List<String> others = availableNames(user.getId()).stream()
                .filter(n -> !selected.contains(n))
                .toList();
        try {
            AiRecipe recipe = recipeAiService.generateAntiWasteRecipe(selected, others, user);
            session.setAttribute(SESSION_AI_RECIPE, recipe);
            model.addAttribute("recipe", recipe);
        } catch (AiUnavailableException | BusinessException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "recipes/ai";
    }

    @PostMapping("/ai/save")
    public String aiSave(@AuthenticationPrincipal PantryUserDetails principal,
                         HttpSession session,
                         RedirectAttributes redirect) {
        Object stored = session.getAttribute(SESSION_AI_RECIPE);
        if (!(stored instanceof AiRecipe aiRecipe)) {
            redirect.addFlashAttribute("error", "Nessuna ricetta da salvare: generane prima una");
            return "redirect:/recipes/ai";
        }
        try {
            Recipe saved = recipeService.saveAiRecipe(principal.getId(), aiRecipe);
            session.removeAttribute(SESSION_AI_RECIPE);
            redirect.addFlashAttribute("success", "\"" + saved.getName() + "\" salvata nel ricettario");
            return "redirect:/recipes/" + saved.getId();
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/recipes/ai";
        }
    }

    /** Ingredienti mostrati come caselle: prima quelli in scadenza (già selezionati), poi gli altri. */
    private void prepareAiPage(Long userId, List<String> selected, Model model) {
        List<PantryItem> items = pantryService.getItems(userId).stream()
                .filter(i -> i.getStatus() != ExpiryStatus.SCADUTO)
                .toList();
        Map<String, PantryItem> expiring = new LinkedHashMap<>();
        Map<String, PantryItem> others = new LinkedHashMap<>();
        for (PantryItem item : items) {
            String name = item.getProduct().getName();
            if (item.getStatus() == ExpiryStatus.IN_SCADENZA) {
                expiring.putIfAbsent(name, item);
            }
        }
        for (PantryItem item : items) {
            String name = item.getProduct().getName();
            if (!expiring.containsKey(name)) {
                others.putIfAbsent(name, item);
            }
        }
        model.addAttribute("expiringItems", expiring.values());
        model.addAttribute("otherItems", others.values());
        model.addAttribute("selected", selected != null ? selected : new ArrayList<>(expiring.keySet()));
        model.addAttribute("active", "ai-recipe");
    }

    private List<String> availableNames(Long userId) {
        return pantryService.getItems(userId).stream()
                .filter(i -> i.getStatus() != ExpiryStatus.SCADUTO)
                .map(i -> i.getProduct().getName())
                .distinct()
                .toList();
    }

    public RecipeController(RecipeService recipeService, RecipeAiService recipeAiService, PantryService pantryService, UserService userService) {
        this.recipeService = recipeService;
        this.recipeAiService = recipeAiService;
        this.pantryService = pantryService;
        this.userService = userService;
    }
}
