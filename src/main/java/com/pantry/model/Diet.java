package com.pantry.model;

/** Regime alimentare: vale sia per il profilo utente sia per le ricette. */
public enum Diet {
    ONNIVORA("Onnivora"),
    VEGETARIANA("Vegetariana"),
    VEGANA("Vegana");

    private final String label;

    /**
     * Indica se una ricetta con il regime indicato va bene per chi segue questo regime.
     * Esempio: un vegetariano può mangiare ricette vegetariane e vegane, non quelle onnivore.
     */
    public boolean accepts(Diet recipeDiet) {
        return recipeDiet.ordinal() >= this.ordinal();
    }

    Diet(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
