package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.dto.RegistrationForm;
import com.pantry.service.BusinessException;
import com.pantry.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal PantryUserDetails user) {
        return user != null ? "redirect:/dashboard" : "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(@AuthenticationPrincipal PantryUserDetails user, Model model) {
        if (user != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult errors,
                           RedirectAttributes redirect) {
        if (!errors.hasFieldErrors("confirmPassword") && form.getPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Le due password non coincidono");
        }
        if (errors.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(form);
        } catch (BusinessException e) {
            errors.reject("register", e.getMessage());
            return "auth/register";
        }
        redirect.addFlashAttribute("success", "Account creato. Ora puoi accedere con " + form.getUsername().trim() + ".");
        return "redirect:/login";
    }

    public AuthController(UserService userService) {
        this.userService = userService;
    }
}
