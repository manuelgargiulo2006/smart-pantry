package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /** Pagina di benvenuto; chi ha già fatto il login va direttamente alla dashboard. */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal PantryUserDetails user) {
        return user != null ? "redirect:/dashboard" : "index";
    }
}
