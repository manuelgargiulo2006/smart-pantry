package com.pantry.controller;

import com.pantry.config.PantryUserDetails;
import com.pantry.dto.PantryItemForm;
import com.pantry.dto.ReceiptForm;
import com.pantry.dto.ReceiptImportForm;
import com.pantry.model.Storage;
import com.pantry.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Importazione dei prodotti dal testo di uno scontrino, letto dall'AI. */
@Controller
@RequestMapping("/receipt")
public class ReceiptController {

    private final ReceiptAiService receiptAiService;
    private final PantryService pantryService;
    private final CatalogService catalogService;

    @GetMapping
    public String form(Model model) {
        model.addAttribute("receiptForm", new ReceiptForm());
        model.addAttribute("active", "receipt");
        return "ai/receipt";
    }

    @PostMapping("/analyze")
    public String analyze(@Valid @ModelAttribute("receiptForm") ReceiptForm receiptForm,
                          BindingResult errors,
                          Model model) {
        model.addAttribute("active", "receipt");
        if (errors.hasErrors()) {
            return "ai/receipt";
        }
        try {
            ReceiptImportForm importForm = receiptAiService.analyze(receiptForm.getText());
            if (importForm.getRows().isEmpty()) {
                model.addAttribute("error", "Non ho trovato prodotti alimentari in questo testo. Controlla di averlo incollato tutto.");
                return "ai/receipt";
            }
            model.addAttribute("importForm", importForm);
            model.addAttribute("products", catalogService.getProducts());
            model.addAttribute("storages", Storage.values());
            model.addAttribute("today", LocalDate.now());
            return "ai/receipt-review";
        } catch (AiUnavailableException e) {
            model.addAttribute("error", e.getMessage());
            return "ai/receipt";
        }
    }

    @PostMapping("/import")
    public String importItems(@AuthenticationPrincipal PantryUserDetails user,
                              @ModelAttribute("importForm") ReceiptImportForm importForm,
                              Model model,
                              RedirectAttributes redirect) {
        List<String> problems = new ArrayList<>();
        List<PantryItemForm> toSave = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < importForm.getRows().size(); i++) {
            ReceiptImportForm.Row row = importForm.getRows().get(i);
            if (!row.isInclude()) {
                continue;
            }
            String label = "Riga " + (i + 1) + " (" + row.getReceiptText() + ")";
            if (row.getProductId() == null) {
                problems.add(label + ": scegli il prodotto del catalogo oppure togli la spunta");
                continue;
            }
            if (row.getQuantity() == null || row.getQuantity() < 1 || row.getQuantity() > 999) {
                problems.add(label + ": la quantità deve essere tra 1 e 999");
                continue;
            }
            if (row.getExpirationDate() == null || row.getExpirationDate().isBefore(today)) {
                problems.add(label + ": indica una data di scadenza da oggi in poi");
                continue;
            }
            PantryItemForm form = new PantryItemForm();
            form.setProductId(row.getProductId());
            form.setQuantity(row.getQuantity());
            form.setPurchaseDate(today);
            form.setExpirationDate(row.getExpirationDate());
            form.setStorage(row.getStorage() == null ? Storage.DISPENSA : row.getStorage());
            toSave.add(form);
        }

        if (!problems.isEmpty() || toSave.isEmpty()) {
            if (toSave.isEmpty() && problems.isEmpty()) {
                problems.add("Seleziona almeno un prodotto da aggiungere");
            }
            model.addAttribute("problems", problems);
            model.addAttribute("importForm", importForm);
            model.addAttribute("products", catalogService.getProducts());
            model.addAttribute("storages", Storage.values());
            model.addAttribute("today", today);
            model.addAttribute("active", "receipt");
            return "ai/receipt-review";
        }

        toSave.forEach(f -> pantryService.addItem(user.getId(), f));
        redirect.addFlashAttribute("success", toSave.size() == 1
                ? "1 prodotto dello scontrino aggiunto alla dispensa"
                : toSave.size() + " prodotti dello scontrino aggiunti alla dispensa");
        return "redirect:/pantry";
    }

    public ReceiptController(ReceiptAiService receiptAiService, PantryService pantryService, CatalogService catalogService) {
        this.receiptAiService = receiptAiService;
        this.pantryService = pantryService;
        this.catalogService = catalogService;
    }
}
