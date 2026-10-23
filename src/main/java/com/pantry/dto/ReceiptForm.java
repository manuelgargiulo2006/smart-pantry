package com.pantry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReceiptForm {

    @NotBlank(message = "Incolla il testo dello scontrino")
    @Size(max = 4000, message = "Il testo è troppo lungo (massimo 4000 caratteri)")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
