package com.pantry.service;

import com.pantry.dto.DashboardData;
import com.pantry.dto.PantryItemForm;
import com.pantry.model.*;
import com.pantry.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Logica della dispensa: inserimento, quantità, scadenze e dati della dashboard. */
@Service
public class PantryService {

    private final PantryItemRepository pantryItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockMovementRepository movementRepository;
    private final ShoppingListService shoppingListService;

    // ---------------------------------------------------------------- lettura

    @Transactional(readOnly = true)
    public List<PantryItem> getItems(Long userId) {
        return pantryItemRepository.findByUserIdOrderByExpirationDateAsc(userId);
    }

    /** Elenco filtrato per testo, categoria e stato di scadenza. */
    @Transactional(readOnly = true)
    public List<PantryItem> search(Long userId, String query, Long categoryId, ExpiryStatus status) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ITALIAN);
        return getItems(userId).stream()
                .filter(i -> q.isEmpty() || i.getProduct().getName().toLowerCase(Locale.ITALIAN).contains(q))
                .filter(i -> categoryId == null || i.getProduct().getCategory().getId().equals(categoryId))
                .filter(i -> status == null || i.getStatus() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public PantryItem getItem(Long userId, Long itemId) {
        return pantryItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Prodotto non trovato nella tua dispensa"));
    }

    /** Prodotti non ancora scaduti che scadono entro 3 giorni. */
    @Transactional(readOnly = true)
    public List<PantryItem> getExpiringSoon(Long userId) {
        return getItems(userId).stream()
                .filter(i -> i.getStatus() == ExpiryStatus.IN_SCADENZA)
                .toList();
    }

    /** Numero di prodotti scaduti o in scadenza: è il numero mostrato nel menu laterale. */
    @Transactional(readOnly = true)
    public long countAlerts(Long userId) {
        LocalDate limit = LocalDate.now().plusDays(ExpiryStatus.GIORNI_PREAVVISO);
        return pantryItemRepository
                .findByUserIdAndExpirationDateLessThanEqualOrderByExpirationDateAsc(userId, limit)
                .size();
    }

    @Transactional(readOnly = true)
    public DashboardData getDashboard(Long userId) {
        List<PantryItem> items = getItems(userId);
        List<PantryItem> expired = items.stream().filter(i -> i.getStatus() == ExpiryStatus.SCADUTO).toList();
        List<PantryItem> expiring = items.stream().filter(i -> i.getStatus() == ExpiryStatus.IN_SCADENZA).toList();

        LocalDate today = LocalDate.now();
        List<DashboardData.WeekDay> week = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            LocalDate day = today.plusDays(d);
            week.add(new DashboardData.WeekDay(day,
                    items.stream().filter(i -> i.getExpirationDate().equals(day)).toList()));
        }

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        return new DashboardData(
                expired,
                expiring,
                week,
                items.stream().mapToInt(PantryItem::getQuantity).sum(),
                shoppingListService.countOpen(userId),
                movementRepository.sumQuantitySince(userId, MovementType.CONSUMATO, since),
                movementRepository.sumQuantitySince(userId, MovementType.BUTTATO, since),
                movementRepository.findTop8ByUserIdOrderByCreatedAtDesc(userId));
    }

    // ---------------------------------------------------------------- scrittura

    @Transactional
    public PantryItem addItem(Long userId, PantryItemForm form) {
        validateDates(form);
        Product product = productRepository.findById(form.getProductId())
                .orElseThrow(() -> new NotFoundException("Prodotto non trovato nel catalogo"));
        User user = userRepository.getReferenceById(userId);

        PantryItem item = new PantryItem();
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(form.getQuantity());
        item.setPurchaseDate(form.getPurchaseDate());
        item.setExpirationDate(form.getExpirationDate());
        item.setStorage(form.getStorage());
        pantryItemRepository.save(item);
        movementRepository.save(new StockMovement(user, product, MovementType.ACQUISTATO, form.getQuantity()));

        pantryItemRepository.flush();
        applyThreshold(userId, product.getId(), form.getMinQuantity());
        return item;
    }

    /** Modifica dei dati di una confezione (correzione di un errore di inserimento). */
    @Transactional
    public void updateItem(Long userId, Long itemId, PantryItemForm form) {
        validateDates(form);
        PantryItem item = getItem(userId, itemId);
        Long oldProductId = item.getProduct().getId();
        Product product = productRepository.findById(form.getProductId())
                .orElseThrow(() -> new NotFoundException("Prodotto non trovato nel catalogo"));

        item.setProduct(product);
        item.setQuantity(form.getQuantity());
        item.setPurchaseDate(form.getPurchaseDate());
        item.setExpirationDate(form.getExpirationDate());
        item.setStorage(form.getStorage());
        pantryItemRepository.flush();

        if (!oldProductId.equals(product.getId())) {
            shoppingListService.refreshProduct(userId, oldProductId);
        }
        applyThreshold(userId, product.getId(), form.getMinQuantity());
    }

    /** "Consumato": diminuisce la quantità. A zero la confezione esce dalla dispensa. */
    @Transactional
    public String consume(Long userId, Long itemId, int amount) {
        return decrease(userId, itemId, amount, MovementType.CONSUMATO);
    }

    /** "Buttato": come consumato, ma viene conteggiato come spreco. */
    @Transactional
    public String discard(Long userId, Long itemId, int amount) {
        return decrease(userId, itemId, amount, MovementType.BUTTATO);
    }

    /** "Acquistato": aumenta la quantità di una confezione già presente. */
    @Transactional
    public String increase(Long userId, Long itemId, int amount) {
        if (amount < 1 || amount > 999) {
            throw new BusinessException("Indica una quantità tra 1 e 999");
        }
        PantryItem item = getItem(userId, itemId);
        if (item.getQuantity() + amount > 9999) {
            throw new BusinessException("Quantità troppo alta");
        }
        item.setQuantity(item.getQuantity() + amount);
        movementRepository.save(new StockMovement(item.getUser(), item.getProduct(), MovementType.ACQUISTATO, amount));
        pantryItemRepository.flush();
        shoppingListService.refreshProduct(userId, item.getProduct().getId());
        return "Aggiunt" + (amount == 1 ? "o 1 pezzo" : "i " + amount + " pezzi") + " a " + item.getProduct().getName();
    }

    /** Elimina una confezione senza registrare movimenti (es. inserita per errore). */
    @Transactional
    public String delete(Long userId, Long itemId) {
        PantryItem item = getItem(userId, itemId);
        Long productId = item.getProduct().getId();
        String name = item.getProduct().getName();
        pantryItemRepository.delete(item);
        pantryItemRepository.flush();
        shoppingListService.refreshProduct(userId, productId);
        return name + " eliminato dalla dispensa";
    }

    private String decrease(Long userId, Long itemId, int amount, MovementType type) {
        PantryItem item = getItem(userId, itemId);
        if (amount < 1) {
            throw new BusinessException("Indica una quantità di almeno 1");
        }
        if (amount > item.getQuantity()) {
            throw new BusinessException("Hai solo " + item.getQuantity() + " " + item.getProduct().getName());
        }
        Product product = item.getProduct();
        movementRepository.save(new StockMovement(item.getUser(), product, type, amount));
        int left = item.getQuantity() - amount;
        if (left == 0) {
            pantryItemRepository.delete(item);
        } else {
            item.setQuantity(left);
        }
        pantryItemRepository.flush();
        shoppingListService.refreshProduct(userId, product.getId());

        String verb = type == MovementType.BUTTATO ? "buttato" : "consumato";
        return left == 0
                ? product.getName() + " " + verb + " e tolto dalla dispensa"
                : product.getName() + " " + verb + ": ne restano " + left;
    }

    /** Salva (o rimuove se vuota) la soglia minima e ricalcola la lista della spesa. */
    private void applyThreshold(Long userId, Long productId, Integer minQuantity) {
        if (minQuantity != null) {
            shoppingListService.setThreshold(userId, productId, minQuantity);
        } else {
            shoppingListService.refreshProduct(userId, productId);
        }
    }

    private void validateDates(PantryItemForm form) {
        if (form.getExpirationDate().isBefore(form.getPurchaseDate())) {
            throw new BusinessException("La data di scadenza non può precedere la data di acquisto");
        }
    }

    public PantryService(PantryItemRepository pantryItemRepository, ProductRepository productRepository, UserRepository userRepository, StockMovementRepository movementRepository, ShoppingListService shoppingListService) {
        this.pantryItemRepository = pantryItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.movementRepository = movementRepository;
        this.shoppingListService = shoppingListService;
    }
}
