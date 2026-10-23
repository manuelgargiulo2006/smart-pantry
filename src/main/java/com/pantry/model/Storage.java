package com.pantry.model;

/** Dove è conservato un prodotto: serve anche all'AI per stimare la scadenza. */
public enum Storage {
    DISPENSA("Dispensa"),
    FRIGO("Frigorifero"),
    FREEZER("Freezer");

    private final String label;

    Storage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
