package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.dto.PasswordForm;
import com.pantry.dto.ProfileForm;
import com.pantry.model.Allergen;
import com.pantry.model.Diet;
import com.pantry.model.User;
import com.pantry.service.BusinessException;
import com.pantry.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String profile(@AuthenticationPrincipal PantryUserDetails principal, Model model) {
        User user = userService.getUser(principal.getId());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", userService.toProfileForm(user));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
        return show(user, model);
    }

    @PostMapping
    public String update(@AuthenticationPrincipal PantryUserDetails principal,
                         @Valid @ModelAttribute("profileForm") ProfileForm form,
                         BindingResult errors,
                         Model model,
                         RedirectAttributes redirect) {
        User user = userService.getUser(principal.getId());
        if (!errors.hasErrors()) {
            try {
                userService.updateProfile(user.getId(), form);
                redirect.addFlashAttribute("success", "Profilo aggiornato");
                return "redirect:/profile";
            } catch (BusinessException e) {
                errors.rejectValue("email", "duplicate", e.getMessage());
            }
        }
        model.addAttribute("passwordForm", new PasswordForm());
        return show(user, model);
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal PantryUserDetails principal,
                                 @Valid @ModelAttribute("passwordForm") PasswordForm form,
                                 BindingResult errors,
                                 Model model,
                                 RedirectAttributes redirect) {
        User user = userService.getUser(principal.getId());
        if (!errors.hasErrors()) {
            try {
                userService.changePassword(user.getId(), form);
                redirect.addFlashAttribute("success", "Password cambiata");
                return "redirect:/profile";
            } catch (BusinessException e) {
                errors.reject("password", e.getMessage());
            }
        }
        model.addAttribute("profileForm", userService.toProfileForm(user));
        return show(user, model);
    }

    private String show(User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("diets", Diet.values());
        model.addAttribute("allergens", Allergen.values());
        model.addAttribute("active", "profile");
        return "profile";
    }

    public ProfileController(UserService userService) {
        this.userService = userService;
    }
}
