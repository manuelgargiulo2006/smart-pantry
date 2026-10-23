package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.dto.ShoppingItemForm;
import com.pantry.dto.ThresholdForm;
import com.pantry.model.PantryItem;
import com.pantry.model.Storage;
import com.pantry.service.BusinessException;
import com.pantry.service.CatalogService;
import com.pantry.service.PantryService;
import com.pantry.service.ShoppingListService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/shopping")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final CatalogService catalogService;
    private final PantryService pantryService;

    @GetMapping
    public String list(@AuthenticationPrincipal PantryUserDetails user, Model model) {
        if (!model.containsAttribute("itemForm")) {
            model.addAttribute("itemForm", new ShoppingItemForm());
        }
        if (!model.containsAttribute("thresholdForm")) {
            model.addAttribute("thresholdForm", new ThresholdForm());
        }
        var openItems = shoppingListService.getOpenItems(user.getId());
        // propone dove conservare ogni prodotto: come l'ultima volta, altrimenti in base alla categoria
        Map<Long, Storage> lastStorage = new HashMap<>();
        pantryService.getItems(user.getId())
                .forEach(p -> lastStorage.put(p.getProduct().getId(), p.getStorage()));
        Map<Long, Storage> storageHints = new HashMap<>();
        openItems.forEach(i -> storageHints.put(i.getId(),
                lastStorage.getOrDefault(i.getProduct().getId(), guessStorage(i.getProduct().getCategory().getName()))));
        model.addAttribute("openItems", openItems);
        model.addAttribute("storageHints", storageHints);
        Map<Long, LocalDate> expiryHints = new HashMap<>();
        openItems.forEach(i -> expiryHints.put(i.getId(),
                ShoppingListService.defaultExpiry(i.getProduct(), LocalDate.now())));
        model.addAttribute("expiryHints", expiryHints);
        model.addAttribute("boughtItems", shoppingListService.getBoughtItems(user.getId()));
        var thresholds = shoppingListService.getThresholds(user.getId());
        Map<Long, Integer> stocks = new HashMap<>();
        thresholds.forEach(t -> stocks.put(t.getProduct().getId(),
                shoppingListService.getStock(user.getId(), t.getProduct().getId())));
        model.addAttribute("thresholds", thresholds);
        model.addAttribute("stocks", stocks);
        model.addAttribute("products", catalogService.getProducts());
        model.addAttribute("storages", Storage.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("active", "shopping");
        return "shopping/list";
    }

    private static Storage guessStorage(String categoryName) {
        String c = categoryName.toLowerCase(Locale.ITALIAN);
        if (c.contains("surgelat")) {
            return Storage.FREEZER;
        }
        if (c.contains("latt") || c.contains("carne") || c.contains("pesce") || c.contains("fresc")) {
            return Storage.FRIGO;
        }
        return Storage.DISPENSA;
    }

    @PostMapping("/add")
    public String add(@AuthenticationPrincipal PantryUserDetails user,
                      @Valid @ModelAttribute("itemForm") ShoppingItemForm form,
                      BindingResult errors,
                      RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", errors.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/shopping";
        }
        shoppingListService.addManual(user.getId(), form.getProductId(), form.getQuantity());
        redirect.addFlashAttribute("success", catalogService.getProduct(form.getProductId()).getName()
                + " aggiunto alla lista");
        return "redirect:/shopping";
    }

    /** Spunta "comprato": il prodotto torna automaticamente in dispensa. */
    @PostMapping("/{id}/buy")
    public String buy(@AuthenticationPrincipal PantryUserDetails user,
                      @PathVariable Long id,
                      @RequestParam(required = false) Integer quantity,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDate,
                      @RequestParam(required = false) Storage storage,
                      RedirectAttributes redirect) {
        try {
            PantryItem item = shoppingListService.markBought(user.getId(), id, quantity, expirationDate, storage);
            redirect.addFlashAttribute("success", item.getProduct().getName() + " comprato e aggiunto alla dispensa");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shopping";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal PantryUserDetails user, @PathVariable Long id,
                         RedirectAttributes redirect) {
        shoppingListService.remove(user.getId(), id);
        redirect.addFlashAttribute("success", "Voce rimossa dalla lista");
        return "redirect:/shopping";
    }

    @PostMapping("/clear-bought")
    public String clearBought(@AuthenticationPrincipal PantryUserDetails user, RedirectAttributes redirect) {
        int removed = shoppingListService.clearBought(user.getId());
        redirect.addFlashAttribute("success", removed == 0
                ? "Non ci sono prodotti comprati da togliere"
                : "Tolti dalla lista " + removed + (removed == 1 ? " prodotto comprato" : " prodotti comprati"));
        return "redirect:/shopping";
    }

    @PostMapping("/refresh")
    public String refresh(@AuthenticationPrincipal PantryUserDetails user, RedirectAttributes redirect) {
        shoppingListService.refreshAll(user.getId());
        redirect.addFlashAttribute("success", "Lista aggiornata in base alle soglie minime");
        return "redirect:/shopping";
    }

    /** Esporta la lista in un file di testo. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal PantryUserDetails user) {
        String text = shoppingListService.exportText(user.getId(), user.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("lista-spesa-" + LocalDate.now() + ".txt")
                        .build().toString())
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(text.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- soglie minime

    @PostMapping("/thresholds")
    public String saveThreshold(@AuthenticationPrincipal PantryUserDetails user,
                                @Valid @ModelAttribute("thresholdForm") ThresholdForm form,
                                BindingResult errors,
                                RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", errors.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/shopping#soglie";
        }
        shoppingListService.setThreshold(user.getId(), form.getProductId(), form.getMinQuantity());
        redirect.addFlashAttribute("success", "Soglia minima salvata per "
                + catalogService.getProduct(form.getProductId()).getName());
        return "redirect:/shopping#soglie";
    }

    @PostMapping("/thresholds/{id}/delete")
    public String deleteThreshold(@AuthenticationPrincipal PantryUserDetails user, @PathVariable Long id,
                                  RedirectAttributes redirect) {
        shoppingListService.deleteThreshold(user.getId(), id);
        redirect.addFlashAttribute("success", "Soglia minima rimossa");
        return "redirect:/shopping#soglie";
    }

    public ShoppingListController(ShoppingListService shoppingListService, CatalogService catalogService, PantryService pantryService) {
        this.shoppingListService = shoppingListService;
        this.catalogService = catalogService;
        this.pantryService = pantryService;
    }
}
