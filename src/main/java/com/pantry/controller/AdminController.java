package com.pantry.controller;

import com.pantry.dto.ProductForm;
import com.pantry.model.Allergen;
import com.pantry.model.Category;
import com.pantry.model.Product;
import com.pantry.model.Role;
import com.pantry.repository.UserRepository;
import com.pantry.service.BusinessException;
import com.pantry.service.CatalogService;
import com.pantry.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/** Area amministratore: catalogo generale dei prodotti, categorie e ricettario. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CatalogService catalogService;
    private final RecipeService recipeService;
    private final UserRepository userRepository;

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("categoryCount", catalogService.getCategories().size());
        model.addAttribute("productCount", catalogService.getProducts().size());
        model.addAttribute("recipeCount", recipeService.getAll().size());
        model.addAttribute("userCount", userRepository.countByRole(Role.USER));
        model.addAttribute("active", "admin");
        return "admin/overview";
    }

    // ---------------------------------------------------------------- categorie

    @GetMapping("/categories")
    public String categories(Model model) {
        Map<Category, Long> categories = new LinkedHashMap<>();
        for (Category c : catalogService.getCategories()) {
            categories.put(c, catalogService.countProducts(c.getId()));
        }
        model.addAttribute("categories", categories);
        model.addAttribute("active", "admin-categories");
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String name, RedirectAttributes redirect) {
        try {
            Category saved = catalogService.saveCategory(null, validName(name));
            redirect.addFlashAttribute("success", "Categoria \"" + saved.getName() + "\" creata");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}")
    public String renameCategory(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
        try {
            Category saved = catalogService.saveCategory(id, validName(name));
            redirect.addFlashAttribute("success", "Categoria rinominata in \"" + saved.getName() + "\"");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            String name = catalogService.getCategory(id).getName();
            catalogService.deleteCategory(id);
            redirect.addFlashAttribute("success", "Categoria \"" + name + "\" eliminata");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Inserisci il nome della categoria");
        }
        if (name.trim().length() > 60) {
            throw new BusinessException("Il nome della categoria può avere al massimo 60 caratteri");
        }
        return name;
    }

    // ---------------------------------------------------------------- prodotti

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Long categoryId,
                           Model model) {
        model.addAttribute("products", catalogService.searchProducts(q, categoryId));
        model.addAttribute("categories", catalogService.getCategories());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("active", "admin-products");
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        return showProductForm(model, new ProductForm(), null);
    }

    @PostMapping("/products/new")
    public String createProduct(@Valid @ModelAttribute("form") ProductForm form,
                                BindingResult errors,
                                Model model,
                                RedirectAttributes redirect) {
        return saveProduct(null, form, errors, model, redirect);
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = catalogService.getProduct(id);
        return showProductForm(model, ProductForm.from(product), product);
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("form") ProductForm form,
                                BindingResult errors,
                                Model model,
                                RedirectAttributes redirect) {
        return saveProduct(id, form, errors, model, redirect);
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            String name = catalogService.getProduct(id).getName();
            catalogService.deleteProduct(id);
            redirect.addFlashAttribute("success", "\"" + name + "\" eliminato dal catalogo");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    private String saveProduct(Long id, ProductForm form, BindingResult errors, Model model,
                               RedirectAttributes redirect) {
        Product existing = id == null ? null : catalogService.getProduct(id);
        if (errors.hasErrors()) {
            return showProductForm(model, form, existing);
        }
        try {
            Product saved = catalogService.saveProduct(id, form);
            redirect.addFlashAttribute("success", "\"" + saved.getName() + "\" "
                    + (id == null ? "aggiunto al catalogo" : "aggiornato"));
            return "redirect:/admin/products";
        } catch (BusinessException e) {
            errors.reject("product", e.getMessage());
            return showProductForm(model, form, existing);
        }
    }

    private String showProductForm(Model model, ProductForm form, Product product) {
        model.addAttribute("form", form);
        model.addAttribute("product", product);
        model.addAttribute("categories", catalogService.getCategories());
        model.addAttribute("allergens", Allergen.values());
        model.addAttribute("active", "admin-products");
        return "admin/product-form";
    }

    // ---------------------------------------------------------------- ricette

    @GetMapping("/recipes")
    public String recipes(Model model) {
        model.addAttribute("recipes", recipeService.getAll());
        model.addAttribute("active", "admin-recipes");
        return "admin/recipes";
    }

    @PostMapping("/recipes/{id}/delete")
    public String deleteRecipe(@PathVariable Long id, RedirectAttributes redirect) {
        String name = recipeService.getRecipe(id).getName();
        recipeService.delete(id);
        redirect.addFlashAttribute("success", "Ricetta \"" + name + "\" eliminata");
        return "redirect:/admin/recipes";
    }

    public AdminController(CatalogService catalogService, RecipeService recipeService, UserRepository userRepository) {
        this.catalogService = catalogService;
        this.recipeService = recipeService;
        this.userRepository = userRepository;
    }
}
