package com.pantry.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Stima della durata di conservazione fatta dall'AI. */
public record ExpiryEstimate(
        @JsonPropertyDescription("Numero di giorni di conservazione a partire dalla data di acquisto")
        int giorni,
        @JsonPropertyDescription("Consiglio pratico di conservazione, una frase")
        String consiglio) {
}
