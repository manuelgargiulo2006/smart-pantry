package com.pantry.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.Serializable;
import java.util.List;

/**
 * Ricetta generata dall'AI. Spring AI (BeanOutputConverter) chiede al modello
 * di rispondere con un JSON che rispetta esattamente questa struttura.
 */
public record AiRecipe(
        @JsonPropertyDescription("Nome della ricetta")
        String nome,
        @JsonPropertyDescription("Breve descrizione del piatto, massimo 2 frasi")
        String descrizione,
        @JsonPropertyDescription("Tempo di preparazione totale in minuti")
        int tempoPreparazione,
        @JsonPropertyDescription("Numero di porzioni")
        int porzioni,
        @JsonPropertyDescription("Regime della ricetta: ONNIVORA, VEGETARIANA oppure VEGANA")
        String regime,
        @JsonPropertyDescription("Tutti gli ingredienti con le quantità")
        List<Ingrediente> ingredienti,
        @JsonPropertyDescription("Ingredienti della ricetta che l'utente non ha in dispensa e deve comprare")
        List<String> ingredientiMancanti,
        @JsonPropertyDescription("Passaggi di preparazione in ordine, uno per elemento")
        List<String> passaggi,
        @JsonPropertyDescription("Un consiglio anti-spreco legato alla ricetta")
        String consiglioAntiSpreco) implements Serializable {

    public record Ingrediente(
            @JsonPropertyDescription("Nome dell'ingrediente")
            String nome,
            @JsonPropertyDescription("Quantità, es. 200 g, 2 cucchiai, q.b.")
            String quantita) implements Serializable {
    }
}
