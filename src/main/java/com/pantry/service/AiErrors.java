package com.pantry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/** Esegue una chiamata all'AI trasformando qualsiasi errore in un messaggio comprensibile. */
final class AiErrors {

    private static final Logger log = LoggerFactory.getLogger(AiErrors.class);

    private AiErrors() {
    }

    static <T> T call(Supplier<T> request) {
        T result;
        try {
            result = request.get();
        } catch (RuntimeException e) {
            log.warn("Chiamata all'AI fallita: {}", e.getMessage());
            throw new AiUnavailableException(describe(e), e);
        }
        if (result == null) {
            throw new AiUnavailableException("L'AI ha restituito una risposta vuota. Riprova tra qualche secondo.", null);
        }
        return result;
    }

    private static String describe(RuntimeException e) {
        String message = String.valueOf(e.getMessage()).toLowerCase();
        if (message.contains("401") || message.contains("invalid_api_key") || message.contains("invalid api key")) {
            return "La chiave di Groq non è valida. Inseriscila in application.properties "
                    + "(spring.ai.openai.api-key) e riavvia l'applicazione.";
        }
        if (message.contains("model_not_found") || message.contains("decommissioned")
                || (message.contains("model") && message.contains("does not exist"))) {
            return "Il modello AI indicato non è più disponibile su Groq. Scegline un altro su "
                    + "console.groq.com/docs/models e aggiorna spring.ai.openai.chat.options.model.";
        }
        if (message.contains("429") || message.contains("rate limit")) {
            return "Hai raggiunto il limite di richieste gratuite di Groq. Aspetta un minuto e riprova.";
        }
        if (message.contains("json") || message.contains("deserializ") || message.contains("convert")) {
            return "L'AI ha risposto in un formato non valido. Riprova: di solito al secondo tentativo funziona.";
        }
        if (message.contains("connect") || message.contains("timed out") || message.contains("unknownhost")
                || message.contains("i/o error")) {
            return "Impossibile raggiungere Groq. Controlla la connessione a internet e riprova.";
        }
        return "Il servizio di intelligenza artificiale non è disponibile in questo momento. Riprova più tardi.";
    }
}
