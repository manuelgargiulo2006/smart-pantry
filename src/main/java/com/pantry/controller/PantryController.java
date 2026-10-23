package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.dto.ExpiryEstimate;
import com.pantry.dto.PantryItemForm;
import com.pantry.model.ExpiryStatus;
import com.pantry.model.PantryItem;
import com.pantry.model.Product;
import com.pantry.model.Storage;
import com.pantry.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@Controller
@RequestMapping("/pantry")
public class PantryController {

    private final PantryService pantryService;
    private final CatalogService catalogService;
    private final ShoppingListService shoppingListService;
    private final ExpiryAiService expiryAiService;

    // ---------------------------------------------------------------- elenco

    @GetMapping
    public String list(@AuthenticationPrincipal PantryUserDetails user,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) ExpiryStatus status,
                       Model model) {
        model.addAttribute("items", pantryService.search(user.getId(), q, categoryId, status));
        model.addAttribute("totalCount", pantryService.getItems(user.getId()).size());
        model.addAttribute("categories", catalogService.getCategories());
        model.addAttribute("statuses", ExpiryStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("active", "pantry");
        return "pantry/list";
    }

    // ---------------------------------------------------------------- inserimento

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long productId, Model model) {
        PantryItemForm form = new PantryItemForm();
        if (productId != null) {
            Product product = catalogService.getProduct(productId);
            form.setProductId(productId);
            form.setExpirationDate(ShoppingListService.defaultExpiry(product, LocalDate.now()));
        }
        return showForm(model, form, null);
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal PantryUserDetails user,
                         @Valid @ModelAttribute("form") PantryItemForm form,
                         BindingResult errors,
                         Model model,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return showForm(model, form, null);
        }
        try {
            PantryItem item = pantryService.addItem(user.getId(), form);
            redirect.addFlashAttribute("success", item.getProduct().getName() + " aggiunto alla dispensa");
            return "redirect:/pantry";
        } catch (BusinessException e) {
            errors.reject("pantry", e.getMessage());
            return showForm(model, form, null);
        }
    }

    // ---------------------------------------------------------------- modifica

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal PantryUserDetails user, @PathVariable Long id, Model model) {
        PantryItem item = pantryService.getItem(user.getId(), id);
        Integer min = shoppingListService.getThresholdValue(user.getId(), item.getProduct().getId());
        return showForm(model, PantryItemForm.from(item, min), item);
    }

    @PostMapping("/{id}/edit")
    public String update(@AuthenticationPrincipal PantryUserDetails user,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("form") PantryItemForm form,
                         BindingResult errors,
                         Model model,
                         RedirectAttributes redirect) {
        PantryItem item = pantryService.getItem(user.getId(), id);
        if (errors.hasErrors()) {
            return showForm(model, form, item);
        }
        try {
            pantryService.updateItem(user.getId(), id, form);
            redirect.addFlashAttribute("success", "Modifiche salvate");
            return "redirect:/pantry";
        } catch (BusinessException e) {
            errors.reject("pantry", e.getMessage());
            return showForm(model, form, item);
        }
    }

    private String showForm(Model model, PantryItemForm form, PantryItem item) {
        model.addAttribute("form", form);
        model.addAttribute("item", item);
        model.addAttribute("categories", catalogService.getCategories());
        model.addAttribute("products", catalogService.getProducts());
        model.addAttribute("storages", Storage.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("active", "pantry");
        return "pantry/form";
    }

    // ---------------------------------------------------------------- quantità

    @PostMapping("/{id}/consume")
    public String consume(@AuthenticationPrincipal PantryUserDetails user, @PathVariable Long id,
                          @RequestParam(defaultValue = "1") int amount,
                          @RequestParam(defaultValue = "/pantry") String back,
                          RedirectAttributes redirect) {
        return run(redirect, back, () -> pantryService.consume(user.getId(), id, amount));
    }

    @PostMapping("/{id}/discard")
    public String discard(@AuthenticationPrincipal PantryUserDetails user, @PathVariable Long id,
                          @RequestParam(defaultValue = "1") int amount,
                          @RequestParam(defaultValue = "/pantry") String back,
                          RedirectAttributes redirect) {
        return run(redirect, back, () -> pantryService.discard(user.getId(), id, amount));
    }

    @PostMapping("/{id}/increase")
    public String increase(@AuthenticationPrincipal PantryUserDetails user, @PathVariable Long id,
                           @RequestParam(defaultValue = "1") int amount,
                           @RequestParam(defaultValue = "/pantry") String back,
                           RedirectAttributes redirect) {
        return run(redirect, back, () -> pantryService.increase(user.getId(), id, amount));
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal PantryUserDetails user, @PathVariable Long id,
                         RedirectAttributes redirect) {
        return run(redirect, "/pantry", () -> pantryService.delete(user.getId(), id));
    }

    private String run(RedirectAttributes redirect, String back, Supplier<String> action) {
        try {
            redirect.addFlashAttribute("success", action.get());
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + safeBack(back);
    }

    /** Accetta solo indirizzi interni all'applicazione, per evitare redirect verso siti esterni. */
    private static String safeBack(String back) {
        if (back == null || !back.startsWith("/") || back.startsWith("//") || back.contains("\\")) {
            return "/pantry";
        }
        return back;
    }

    // ---------------------------------------------------------------- stima AI della scadenza

    /** Chiamato dalla pagina con JavaScript: restituisce la data di scadenza stimata dall'AI. */
    @GetMapping("/estimate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> estimate(@RequestParam Long productId,
                                                        @RequestParam(defaultValue = "DISPENSA") Storage storage,
                                                        @RequestParam(required = false) String purchaseDate) {
        try {
            Product product = catalogService.getProduct(productId);
            LocalDate from = purchaseDate == null || purchaseDate.isBlank() ? LocalDate.now() : LocalDate.parse(purchaseDate);
            ExpiryEstimate estimate = expiryAiService.estimate(product, storage);
            LocalDate date = from.plusDays(estimate.giorni());
            return ResponseEntity.ok(Map.of(
                    "days", estimate.giorni(),
                    "date", date.toString(),
                    "dateLabel", date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALY)),
                    "advice", estimate.consiglio() == null ? "" : estimate.consiglio()));
        } catch (AiUnavailableException | NotFoundException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Data di acquisto non valida"));
        }
    }

    public PantryController(PantryService pantryService, CatalogService catalogService, ShoppingListService shoppingListService, ExpiryAiService expiryAiService) {
        this.pantryService = pantryService;
        this.catalogService = catalogService;
        this.shoppingListService = shoppingListService;
        this.expiryAiService = expiryAiService;
    }
}
