package com.pantry.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpiryStatusTest {

    @Test
    void ieriEScaduto() {
        assertEquals(ExpiryStatus.SCADUTO, ExpiryStatus.fromDaysLeft(-1));
    }

    @Test
    void daOggiAFra3GiorniEInScadenza() {
        assertEquals(ExpiryStatus.IN_SCADENZA, ExpiryStatus.fromDaysLeft(0));
        assertEquals(ExpiryStatus.IN_SCADENZA, ExpiryStatus.fromDaysLeft(3));
    }

    @Test
    void fra4GiorniEAPosto() {
        assertEquals(ExpiryStatus.OK, ExpiryStatus.fromDaysLeft(4));
    }

    @Test
    void ilProdottoCalcolaIGiorniMancanti() {
        PantryItem item = new PantryItem();
        item.setExpirationDate(LocalDate.now().plusDays(2));
        assertEquals(2, item.getDaysLeft());
        assertEquals(ExpiryStatus.IN_SCADENZA, item.getStatus());
    }
}
