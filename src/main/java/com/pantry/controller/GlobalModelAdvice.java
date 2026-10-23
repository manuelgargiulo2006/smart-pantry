package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.service.PantryService;
import com.pantry.service.ShoppingListService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Dati comuni a tutte le pagine: i numerini rossi del menu laterale. */
@ControllerAdvice(basePackages = "com.pantry.controller")
public class GlobalModelAdvice {

    private final PantryService pantryService;
    private final ShoppingListService shoppingListService;

    @ModelAttribute
    public void addNavigationData(@AuthenticationPrincipal PantryUserDetails user, Model model) {
        if (user != null) {
            model.addAttribute("navAlerts", pantryService.countAlerts(user.getId()));
            model.addAttribute("navShopping", shoppingListService.countOpen(user.getId()));
        }
    }

    public GlobalModelAdvice(PantryService pantryService, ShoppingListService shoppingListService) {
        this.pantryService = pantryService;
        this.shoppingListService = shoppingListService;
    }
}
