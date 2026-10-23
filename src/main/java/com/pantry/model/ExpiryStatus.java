package com.pantry.model;

/** Stato di un prodotto rispetto alla data di scadenza. */
public enum ExpiryStatus {
    SCADUTO("Scaduto", "expired"),
    IN_SCADENZA("In scadenza", "expiring"),
    OK("Ok", "ok");

    /** Giorni entro i quali un prodotto è considerato "in scadenza". */
    public static final int GIORNI_PREAVVISO = 3;

    private final String label;
    /** Classe CSS usata nelle pagine (rosso, arancione, verde). */
    private final String cssClass;

    public static ExpiryStatus fromDaysLeft(long daysLeft) {
        if (daysLeft < 0) {
            return SCADUTO;
        }
        if (daysLeft <= GIORNI_PREAVVISO) {
            return IN_SCADENZA;
        }
        return OK;
    }

    ExpiryStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }
}
