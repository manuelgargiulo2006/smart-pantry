package com.pantry.service;

import com.pantry.dto.ReceiptAnalysis;
import com.pantry.dto.ReceiptImportForm;
import com.pantry.model.Product;
import com.pantry.model.Storage;
import com.pantry.repository.ProductRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Riconoscimento dello scontrino: dal testo grezzo (ad esempio ottenuto con un OCR)
 * l'AI estrae i prodotti, le quantità e una stima della conservazione.
 */
@Service
public class ReceiptAiService {

    private final ChatClient chatClient;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ReceiptImportForm analyze(String receiptText) {
        List<Product> catalog = productRepository.findAllByOrderByNameAsc();
        String catalogNames = catalog.stream().map(Product::getName).collect(Collectors.joining(", "));

        String prompt = """
                Estrai i prodotti alimentari e le quantità da questo scontrino italiano.
                Le abbreviazioni vanno interpretate (es. "LATTE UHT PS" = latte UHT parzialmente scremato).
                Ignora righe che non sono alimenti: totale, resto, IVA, sacchetti, sconti, carte fedeltà, pagamento.
                Se una riga indica più pezzi (es. "2x" o "2 X 1,50") usa quel numero come quantità, altrimenti 1.
                Per ogni prodotto scegli, se esiste, il nome ESATTO corrispondente tra questi prodotti del catalogo:
                %s
                Se nessun prodotto del catalogo corrisponde, metti null in prodottoCatalogo.
                Stima anche dove conservarlo e per quanti giorni si conserva da oggi.

                Testo dello scontrino:
                ---
                %s
                ---
                """.formatted(catalogNames, receiptText.trim());

        ReceiptAnalysis analysis = AiErrors.call(() -> chatClient.prompt()
                .user(prompt)
                .call()
                .entity(ReceiptAnalysis.class));

        Map<String, Product> byName = catalog.stream()
                .collect(Collectors.toMap(p -> p.getName().toLowerCase(Locale.ITALIAN), Function.identity()));

        ReceiptImportForm form = new ReceiptImportForm();
        LocalDate today = LocalDate.now();
        if (analysis.prodotti() == null) {
            return form;
        }
        for (ReceiptAnalysis.Riga line : analysis.prodotti()) {
            Optional<Product> product = find(byName, line.prodottoCatalogo())
                    .or(() -> find(byName, line.nomeProdotto()));

            ReceiptImportForm.Row row = new ReceiptImportForm.Row();
            row.setReceiptText(firstNonBlank(line.testoScontrino(), line.nomeProdotto(), "Prodotto"));
            row.setProductId(product.map(Product::getId).orElse(null));
            row.setInclude(product.isPresent());
            row.setQuantity(Math.min(99, Math.max(1, line.quantita())));
            row.setStorage(parseStorage(line.conservazione()));
            int days = line.giorniConservazione() > 0
                    ? Math.min(line.giorniConservazione(), 3650)
                    : product.map(p -> p.getShelfLifeDays() == null ? 7 : p.getShelfLifeDays()).orElse(7);
            row.setExpirationDate(today.plusDays(days));
            form.getRows().add(row);
        }
        return form;
    }

    private static Optional<Product> find(Map<String, Product> byName, String name) {
        if (name == null || name.isBlank() || "null".equalsIgnoreCase(name.trim())) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name.trim().toLowerCase(Locale.ITALIAN)));
    }

    static Storage parseStorage(String value) {
        if (value == null) {
            return Storage.DISPENSA;
        }
        try {
            return Storage.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Storage.DISPENSA;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    public ReceiptAiService(ChatClient chatClient, ProductRepository productRepository) {
        this.chatClient = chatClient;
        this.productRepository = productRepository;
    }
}
