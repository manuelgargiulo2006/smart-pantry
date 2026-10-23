package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.model.User;
import com.pantry.service.PantryService;
import com.pantry.service.RecipeService;
import com.pantry.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final PantryService pantryService;
    private final RecipeService recipeService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal PantryUserDetails principal, Model model) {
        User user = userService.getUser(principal.getId());
        model.addAttribute("data", pantryService.getDashboard(user.getId()));
        model.addAttribute("suggestions", recipeService.suggest(user).stream().limit(3).toList());
        model.addAttribute("active", "dashboard");
        return "dashboard";
    }

    public DashboardController(PantryService pantryService, RecipeService recipeService, UserService userService) {
        this.pantryService = pantryService;
        this.recipeService = recipeService;
        this.userService = userService;
    }
}
