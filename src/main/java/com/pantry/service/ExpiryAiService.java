package com.pantry.service;

import com.pantry.dto.ExpiryEstimate;
import com.pantry.model.Product;
import com.pantry.model.Storage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Stima della data di scadenza per i cibi freschi senza codice a barre
 * (frutta, verdura, carne e pesce al banco...).
 */
@Service
public class ExpiryAiService {

    private final ChatClient chatClient;

    public ExpiryEstimate estimate(Product product, Storage storage) {
        String prompt = """
                Ho comprato oggi questo prodotto: "%s" (categoria: %s).
                Lo conservo in: %s.
                Qual è la durata media di conservazione in giorni, considerando un prodotto fresco appena acquistato?
                Sii prudente: è meglio una stima leggermente corta che rischiare di mangiare cibo andato a male.
                """.formatted(product.getName(), product.getCategory().getName(), storage.getLabel());

        ExpiryEstimate estimate = AiErrors.call(() -> chatClient.prompt()
                .user(prompt)
                .call()
                .entity(ExpiryEstimate.class));

        int days = Math.max(1, Math.min(estimate.giorni(), 3650));
        return new ExpiryEstimate(days, estimate.consiglio());
    }

    public ExpiryAiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}
