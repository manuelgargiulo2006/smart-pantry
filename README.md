# Smart Pantry (Dispensa Intelligente)

Web application per la gestione degli sprechi alimentari domestici e la pianificazione della spesa.
Java 17+, Spring Boot 3.5 (Web, Data JPA, Security), Thymeleaf, MySQL, Maven, Lombok, Spring AI con Groq.

## Avvio

1. **MySQL** deve essere avviato (servizio MySQL80). Il database `smart_pantry_db` viene creato da solo
   al primo avvio; in alternativa si può eseguire a mano `src/main/resources/schema.sql` in MySQL Workbench.
2. In `src/main/resources/application.properties` controllare utente e password di MySQL.
   La chiave Groq (gratuita, da https://console.groq.com/keys) **non va scritta nel codice**:
   impostarla nella variabile d'ambiente `GROQ_API_KEY`
   (in Eclipse: Run Configurations → scheda Environment).
3. Avviare `SmartPantryApplication` e aprire http://localhost:8080

Account creati al primo avvio:

| Utente | Password  | Ruolo |
|--------|-----------|-------|
| admin  | admin1234 | ADMIN: gestisce catalogo prodotti, categorie e ricettario |
| demo   | demo1234  | USER: dispensa già pronta con prodotti scaduti e in scadenza |

## Funzionalità

- **Utenti**: registrazione, login (password cifrate con BCrypt), profilo con regime alimentare,
  14 allergeni e preferenze; ruoli USER e ADMIN.
- **Dispensa**: inserimento con prodotto, categoria, data di acquisto e scadenza (anche tramite codice a barre);
  quantità con *consumato / buttato / acquistato*; dashboard con scaduti in rosso e in scadenza entro 3 giorni in arancione.
- **Lista della spesa automatica**: quando la scorta scende sotto la soglia minima scelta dall'utente il prodotto
  entra in lista con la quantità mancante; esportazione in file di testo; la spunta "Comprato" riporta il prodotto in dispensa.
- **Ricette**: incrocio tra i prodotti in scadenza e il database di ricette, escludendo quelle non adatte
  al regime alimentare o con allergeni dichiarati.
- **Intelligenza artificiale (Spring AI + Groq)**:
  - generatore di ricette anti-spreco con risposta strutturata in oggetto Java (`AiRecipe`, BeanOutputConverter);
  - lettura del testo di uno scontrino con riconoscimento di prodotti e quantità;
  - stima della data di scadenza per i prodotti freschi.

## API REST

`POST /api/ai/recipe` con autenticazione Basic (es. `demo` / `demo1234`) e body JSON:

```json
["Zucchine", "Uova", "Panna da cucina"]
```

## Struttura

```
com.pantry.config      sicurezza, configurazione AI, dati iniziali
com.pantry.model       entity JPA (users, categories, products, pantry_items, shopping_list, ...)
com.pantry.repository  interfacce JpaRepository
com.pantry.service     logica di business e servizi AI
com.pantry.controller  controller Thymeleaf e REST
com.pantry.dto         dati dei form e risposte strutturate dell'AI
```

## Database

Tabelle richieste: `users`, `categories`, `products`, `pantry_items`, `shopping_list`.
Tabelle aggiuntive necessarie alle funzionalità: `user_allergens`, `product_allergens`,
`stock_thresholds` (soglie minime), `stock_movements` (storico consumato/buttato/acquistato),
`recipes` e `recipe_ingredients` (database di ricette).

Se Groq segnala che il modello non esiste più, sceglierne un altro su
https://console.groq.com/docs/models e aggiornare `spring.ai.openai.chat.options.model`.
