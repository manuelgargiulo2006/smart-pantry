package com.pantry.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Prodotti riconosciuti dall'AI nel testo di uno scontrino. */
public record ReceiptAnalysis(
        @JsonPropertyDescription("Un elemento per ogni prodotto alimentare dello scontrino")
        List<Riga> prodotti) {

    public record Riga(
            @JsonPropertyDescription("La riga così come compare nello scontrino")
            String testoScontrino,
            @JsonPropertyDescription("Nome del prodotto scritto per esteso in italiano")
            String nomeProdotto,
            @JsonPropertyDescription("Nome ESATTO di un prodotto del catalogo che corrisponde, oppure null se nessuno corrisponde")
            String prodottoCatalogo,
            @JsonPropertyDescription("Numero di confezioni acquistate")
            int quantita,
            @JsonPropertyDescription("Dove conservarlo: DISPENSA, FRIGO oppure FREEZER")
            String conservazione,
            @JsonPropertyDescription("Giorni di conservazione stimati da oggi")
            int giorniConservazione) {
    }
}
