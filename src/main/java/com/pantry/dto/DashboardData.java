package com.pantry.dto;

import com.pantry.model.PantryItem;
import com.pantry.model.StockMovement;

import java.util.List;

/** Tutto quello che serve alla dashboard. */
public record DashboardData(List<PantryItem> expired,
                            List<PantryItem> expiring,
                            List<WeekDay> week,
                            int totalItems,
                            long shoppingCount,
                            long consumedLast30,
                            long wastedLast30,
                            List<StockMovement> recentMovements) {

    /** Un giorno della striscia "prossimi 7 giorni" con i prodotti che scadono quel giorno. */
    public record WeekDay(java.time.LocalDate date, List<PantryItem> items) {
    }

    /** Percentuale di prodotti consumati sul totale tra consumati e buttati (ultimi 30 giorni). */
    public int savedPercent() {
        long total = consumedLast30 + wastedLast30;
        return total == 0 ? 100 : (int) Math.round(consumedLast30 * 100.0 / total);
    }
}
