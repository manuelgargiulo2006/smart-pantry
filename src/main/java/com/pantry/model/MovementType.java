package com.pantry.model;

/** Tipi di movimento sulle scorte. */
public enum MovementType {
    ACQUISTATO("Acquistato"),
    CONSUMATO("Consumato"),
    BUTTATO("Buttato");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
