package com.pantry.model;

/** I 14 allergeni che per legge (Reg. UE 1169/2011) vanno segnalati negli alimenti. */
public enum Allergen {
    GLUTINE("Glutine"),
    CROSTACEI("Crostacei"),
    UOVA("Uova"),
    PESCE("Pesce"),
    ARACHIDI("Arachidi"),
    SOIA("Soia"),
    LATTE("Latte e lattosio"),
    FRUTTA_A_GUSCIO("Frutta a guscio"),
    SEDANO("Sedano"),
    SENAPE("Senape"),
    SESAMO("Sesamo"),
    SOLFITI("Solfiti"),
    LUPINI("Lupini"),
    MOLLUSCHI("Molluschi");

    private final String label;

    Allergen(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
