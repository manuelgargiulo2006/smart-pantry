package com.pantry.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Una confezione (o un gruppo di confezioni con la stessa scadenza) presente nella dispensa di un utente.
 * Lo stesso prodotto può comparire più volte con scadenze diverse.
 */
@Entity
@Table(name = "pantry_items")
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Storage storage = Storage.DISPENSA;

    /** Giorni mancanti alla scadenza (negativo se già scaduto). */
    public long getDaysLeft() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    public ExpiryStatus getStatus() {
        return ExpiryStatus.fromDaysLeft(getDaysLeft());
    }

    public PantryItem() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
