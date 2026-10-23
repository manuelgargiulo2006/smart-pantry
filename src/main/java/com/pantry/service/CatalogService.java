package com.pantry.service;

import com.pantry.dto.ProductForm;
import com.pantry.model.Category;
import com.pantry.model.Product;
import com.pantry.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/** Catalogo generale: categorie e prodotti (modificabili solo dall'admin). */
@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PantryItemRepository pantryItemRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final StockThresholdRepository thresholdRepository;
    private final StockMovementRepository movementRepository;

    // ---------------------------------------------------------------- categorie

    @Transactional(readOnly = true)
    public List<Category> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria non trovata"));
    }

    @Transactional(readOnly = true)
    public long countProducts(Long categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }

    @Transactional
    public Category saveCategory(Long id, String rawName) {
        String name = capitalize(rawName);
        boolean duplicate = id == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicate) {
            throw new BusinessException("La categoria \"" + name + "\" esiste già");
        }
        Category category = id == null ? new Category() : getCategory(id);
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategory(id);
        long products = productRepository.countByCategoryId(id);
        if (products > 0) {
            throw new BusinessException("Impossibile eliminare \"" + category.getName() + "\": contiene "
                    + products + (products == 1 ? " prodotto" : " prodotti")
                    + ". Sposta o elimina prima i prodotti.");
        }
        categoryRepository.delete(category);
    }

    // ---------------------------------------------------------------- prodotti

    @Transactional(readOnly = true)
    public List<Product> getProducts() {
        return productRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String query, Long categoryId) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ITALIAN);
        List<Product> base = categoryId == null
                ? productRepository.findAllByOrderByNameAsc()
                : productRepository.findByCategoryIdOrderByNameAsc(categoryId);
        if (q.isEmpty()) {
            return base;
        }
        return base.stream()
                .filter(p -> p.getName().toLowerCase(Locale.ITALIAN).contains(q)
                        || (p.getBarcode() != null && p.getBarcode().contains(q)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prodotto non trovato"));
    }

    @Transactional
    public Product saveProduct(Long id, ProductForm data) {
        String name = capitalize(data.getName());
        String barcode = StringUtils.hasText(data.getBarcode()) ? data.getBarcode().trim() : null;

        boolean duplicateName = id == null
                ? productRepository.existsByNameIgnoreCase(name)
                : productRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicateName) {
            throw new BusinessException("Il prodotto \"" + name + "\" è già nel catalogo");
        }
        if (barcode != null) {
            boolean duplicateBarcode = id == null
                    ? productRepository.existsByBarcode(barcode)
                    : productRepository.existsByBarcodeAndIdNot(barcode, id);
            if (duplicateBarcode) {
                throw new BusinessException("Il codice a barre " + barcode + " è già assegnato a un altro prodotto");
            }
        }

        Product product = id == null ? new Product() : getProduct(id);
        product.setName(name);
        product.setBarcode(barcode);
        product.setCategory(getCategory(data.getCategoryId()));
        product.setShelfLifeDays(data.getShelfLifeDays());
        product.getAllergens().clear();
        if (data.getAllergens() != null) {
            product.getAllergens().addAll(data.getAllergens());
        }
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        if (pantryItemRepository.existsByProductId(id)
                || shoppingListRepository.existsByProductId(id)
                || thresholdRepository.existsByProductId(id)
                || movementRepository.existsByProductId(id)) {
            throw new BusinessException("Impossibile eliminare \"" + product.getName()
                    + "\": è già usato nelle dispense o nelle liste della spesa degli utenti.");
        }
        productRepository.delete(product);
    }

    /** "  latte intero " diventa "Latte intero". */
    static String capitalize(String value) {
        String trimmed = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            throw new BusinessException("Il nome non può essere vuoto");
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ITALIAN) + trimmed.substring(1);
    }

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository, PantryItemRepository pantryItemRepository, ShoppingListRepository shoppingListRepository, StockThresholdRepository thresholdRepository, StockMovementRepository movementRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.thresholdRepository = thresholdRepository;
        this.movementRepository = movementRepository;
    }
}
