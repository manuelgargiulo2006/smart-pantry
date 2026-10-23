package com.pantry.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pantry.model.*;
import com.pantry.repository.*;
import com.pantry.service.ShoppingListService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.pantry.model.Allergen.*;

/**
 * Popola il database al primo avvio con categorie, catalogo prodotti, ricette
 * e due account di prova. Ogni blocco viene inserito solo se la tabella è vuota,
 * quindi i dati non si duplicano ai riavvii successivi.
 *
 * Account creati:
 *   admin / admin1234  (ruolo ADMIN)
 *   demo  / demo1234   (ruolo USER, con una dispensa già pronta)
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PantryItemRepository pantryItemRepository;
    private final StockThresholdRepository thresholdRepository;
    private final StockMovementRepository movementRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListService shoppingListService;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, Product> products = new HashMap<>();

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            loadCatalog();
            log.info("Catalogo iniziale caricato: {} categorie, {} prodotti",
                    categoryRepository.count(), productRepository.count());
        }
        productRepository.findAll().forEach(p -> products.put(p.getName(), p));

        if (recipeRepository.count() == 0) {
            loadRecipes();
            log.info("Ricettario iniziale caricato: {} ricette", recipeRepository.count());
        }
        if (userRepository.count() == 0) {
            loadUsers();
            log.info("Creati gli account di prova: admin/admin1234 e demo/demo1234");
        }
    }

    // ------------------------------------------------------------------ catalogo

    private void loadCatalog() {
        Category frutta = category("Frutta e verdura");
        Category latticini = category("Latticini e uova");
        Category carne = category("Carne e pesce");
        Category freschi = category("Freschi");
        Category pasta = category("Pasta, riso e cereali");
        Category scatolame = category("Scatolame");
        Category surgelati = category("Surgelati");
        Category condimenti = category("Condimenti");
        Category bevande = category("Bevande");

        product("Pomodori", null, frutta, 7);
        product("Zucchine", null, frutta, 6);
        product("Melanzane", null, frutta, 6);
        product("Peperoni", null, frutta, 7);
        product("Patate", null, frutta, 30);
        product("Cipolle", null, frutta, 30);
        product("Carote", null, frutta, 14);
        product("Insalata", null, frutta, 4);
        product("Spinaci", null, frutta, 4);
        product("Funghi champignon", null, frutta, 4);
        product("Banane", null, frutta, 5);
        product("Mele", null, frutta, 21);
        product("Limoni", null, frutta, 21);
        product("Fragole", null, frutta, 3);
        product("Basilico", null, frutta, 5);
        product("Aglio", null, frutta, 60);

        product("Latte intero", "8001234567890", latticini, 7, LATTE);
        product("Mozzarella", "8001234567891", latticini, 5, LATTE);
        product("Parmigiano Reggiano", "8001234567892", latticini, 60, LATTE);
        product("Yogurt bianco", "8001234567893", latticini, 14, LATTE);
        product("Burro", "8001234567894", latticini, 30, LATTE);
        product("Panna da cucina", "8001234567895", latticini, 10, LATTE);
        product("Ricotta", "8001234567896", latticini, 5, LATTE);
        product("Uova", "8001234567897", latticini, 21, UOVA);

        product("Petto di pollo", null, carne, 2);
        product("Carne macinata", null, carne, 2);
        product("Salmone fresco", null, carne, 2, PESCE);
        product("Gamberi", null, carne, 2, CROSTACEI);

        product("Prosciutto cotto", "8001234567900", freschi, 5);
        product("Pasta fresca all'uovo", "8001234567901", freschi, 7, GLUTINE, UOVA);
        product("Pane", null, freschi, 3, GLUTINE);

        product("Spaghetti", "8001234567910", pasta, 730, GLUTINE);
        product("Penne", "8001234567911", pasta, 730, GLUTINE);
        product("Riso Carnaroli", "8001234567912", pasta, 540);
        product("Farina 00", "8001234567913", pasta, 365, GLUTINE);

        product("Passata di pomodoro", "8001234567920", scatolame, 365);
        product("Tonno in scatola", "8001234567921", scatolame, 1095, PESCE);
        product("Ceci in scatola", "8001234567922", scatolame, 1095);
        product("Fagioli borlotti in scatola", "8001234567923", scatolame, 1095);
        product("Mais in scatola", "8001234567924", scatolame, 1095);

        product("Piselli surgelati", "8001234567930", surgelati, 365);
        product("Spinaci surgelati", "8001234567931", surgelati, 365);
        product("Bastoncini di pesce", "8001234567932", surgelati, 365, PESCE, GLUTINE);

        product("Olio extravergine d'oliva", "8001234567940", condimenti, 540);
        product("Pesto alla genovese", "8001234567941", condimenti, 30, LATTE, FRUTTA_A_GUSCIO);

        product("Acqua naturale", "8001234567950", bevande, 365);
        product("Succo d'arancia", "8001234567951", bevande, 10);
    }

    private Category category(String name) {
        return categoryRepository.save(new Category(name));
    }

    private void product(String name, String barcode, Category category, int shelfLifeDays, Allergen... allergens) {
        Set<Allergen> set = allergens.length == 0 ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(Arrays.asList(allergens));
        productRepository.save(new Product(name, barcode, category, shelfLifeDays, set));
    }

    // ------------------------------------------------------------------ ricette

    private void loadRecipes() {
        recipe("Frittata di zucchine", "Morbida e veloce, perfetta per salvare zucchine e uova.",
                Diet.VEGETARIANA, 20, 2,
                List.of("Lava le zucchine e tagliale a rondelle sottili.",
                        "Affetta la cipolla e falla appassire in padella con un filo d'olio.",
                        "Aggiungi le zucchine e cuocile 8 minuti a fuoco medio, salando leggermente.",
                        "Sbatti le uova con il parmigiano e un pizzico di sale.",
                        "Versa le uova sulle verdure e cuoci a fuoco basso con il coperchio per 5 minuti.",
                        "Gira la frittata aiutandoti con un piatto e cuoci ancora 2 minuti."),
                ing("Uova", "4"), ing("Zucchine", "2"), ing("Parmigiano Reggiano", "40 g"),
                ing("Cipolle", "1/2"), ing("Olio extravergine d'oliva", "2 cucchiai"), basic("Sale", "q.b."));

        recipe("Spaghetti al pomodoro e basilico", "Il classico che non tramonta mai.",
                Diet.VEGANA, 20, 4,
                List.of("Metti a bollire l'acqua salata per la pasta.",
                        "In una padella scalda l'olio con lo spicchio d'aglio schiacciato.",
                        "Aggiungi la passata, sala e cuoci 10 minuti.",
                        "Cuoci gli spaghetti, scolali al dente e saltali nel sugo.",
                        "Completa con il basilico fresco spezzettato a mano."),
                ing("Spaghetti", "320 g"), ing("Passata di pomodoro", "400 g"), ing("Basilico", "1 mazzetto"),
                ing("Aglio", "1 spicchio"), ing("Olio extravergine d'oliva", "3 cucchiai"), basic("Sale", "q.b."));

        recipe("Risotto ai funghi", "Cremoso e profumato, ideale per i funghi che non durano molto.",
                Diet.VEGETARIANA, 35, 4,
                List.of("Pulisci i funghi con un panno umido e tagliali a fettine.",
                        "Trita la cipolla e falla appassire con metà del burro.",
                        "Aggiungi i funghi e cuocili 5 minuti.",
                        "Unisci il riso, tostalo 2 minuti e poi aggiungi il brodo caldo un mestolo alla volta.",
                        "Dopo circa 18 minuti spegni e manteca con il burro rimasto e il parmigiano."),
                ing("Riso Carnaroli", "320 g"), ing("Funghi champignon", "300 g"), ing("Cipolle", "1"),
                ing("Burro", "30 g"), ing("Parmigiano Reggiano", "50 g"), basic("Brodo vegetale", "1 l"));

        recipe("Insalata di pollo e mais", "Un piatto unico fresco per usare il pollo prima che scada.",
                Diet.ONNIVORA, 25, 2,
                List.of("Cuoci il petto di pollo sulla piastra 5 minuti per lato e lascialo intiepidire.",
                        "Lava l'insalata e i pomodori, poi tagliali.",
                        "Taglia il pollo a striscioline.",
                        "Unisci tutto con il mais scolato e condisci con olio, limone e sale."),
                ing("Petto di pollo", "300 g"), ing("Insalata", "1 cespo"), ing("Pomodori", "2"),
                ing("Mais in scatola", "1 scatoletta"), ing("Limoni", "1/2"),
                ing("Olio extravergine d'oliva", "2 cucchiai"), basic("Sale", "q.b."));

        recipe("Pasta e ceci", "Piatto povero della tradizione, sostanzioso e saporito.",
                Diet.VEGANA, 30, 4,
                List.of("Trita cipolla e aglio e falli soffriggere nell'olio.",
                        "Aggiungi i ceci scolati e la passata, poi copri con acqua calda.",
                        "Dopo 10 minuti frulla metà dei ceci per rendere la base cremosa.",
                        "Cuoci le penne direttamente nella pentola, aggiungendo acqua se serve.",
                        "Servi con un filo d'olio a crudo e pepe."),
                ing("Ceci in scatola", "2 scatole"), ing("Penne", "250 g"), ing("Cipolle", "1"),
                ing("Aglio", "1 spicchio"), ing("Passata di pomodoro", "3 cucchiai"),
                ing("Olio extravergine d'oliva", "3 cucchiai"), basic("Rosmarino", "1 rametto"));

        recipe("Frullato di banana e fragole", "Merenda veloce con la frutta troppo matura.",
                Diet.VEGETARIANA, 5, 2,
                List.of("Sbuccia le banane e lava le fragole togliendo il picciolo.",
                        "Metti la frutta nel frullatore con lo yogurt e il latte.",
                        "Frulla per un minuto e servi subito."),
                ing("Banane", "2"), ing("Fragole", "200 g"), ing("Yogurt bianco", "1 vasetto"),
                ing("Latte intero", "100 ml"));

        recipe("Polpette al sugo", "Recuperano anche il pane raffermo.",
                Diet.ONNIVORA, 45, 4,
                List.of("Ammolla il pane nel latte e strizzalo.",
                        "Impasta la carne con uovo, pane, parmigiano e sale.",
                        "Forma le polpette con le mani leggermente umide.",
                        "Cuoci la passata con un filo d'olio per 5 minuti.",
                        "Aggiungi le polpette e cuoci a fuoco basso per 25 minuti, girandole a metà cottura."),
                ing("Carne macinata", "400 g"), ing("Uova", "1"), ing("Pane", "1 fetta raffermo"),
                ing("Parmigiano Reggiano", "40 g"), ing("Latte intero", "50 ml"),
                ing("Passata di pomodoro", "500 g"), ing("Olio extravergine d'oliva", "2 cucchiai"));

        recipe("Peperonata con patate", "Contorno ricco che si conserva bene anche il giorno dopo.",
                Diet.VEGANA, 40, 4,
                List.of("Taglia i peperoni a falde, le patate a cubetti e la cipolla a fette.",
                        "Fai appassire la cipolla nell'olio.",
                        "Aggiungi peperoni e patate e cuoci 10 minuti.",
                        "Unisci i pomodori a pezzi, sala e cuoci con il coperchio per 20 minuti."),
                ing("Peperoni", "3"), ing("Patate", "2"), ing("Cipolle", "1"), ing("Pomodori", "2"),
                ing("Olio extravergine d'oliva", "4 cucchiai"), basic("Sale", "q.b."));

        recipe("Salmone al limone con patate", "Secondo leggero cotto in forno.",
                Diet.ONNIVORA, 35, 2,
                List.of("Scalda il forno a 200 gradi.",
                        "Taglia le patate a spicchi e cuocile 15 minuti con olio e sale.",
                        "Aggiungi i tranci di salmone con fettine di limone sopra.",
                        "Cuoci altri 12 minuti e servi."),
                ing("Salmone fresco", "2 tranci"), ing("Patate", "400 g"), ing("Limoni", "1"),
                ing("Olio extravergine d'oliva", "2 cucchiai"), basic("Sale", "q.b."));

        recipe("Caprese", "Pomodori e mozzarella al massimo della freschezza.",
                Diet.VEGETARIANA, 10, 2,
                List.of("Taglia pomodori e mozzarella a fette dello stesso spessore.",
                        "Alternali nel piatto con le foglie di basilico.",
                        "Condisci con olio, sale e origano."),
                ing("Pomodori", "3"), ing("Mozzarella", "250 g"), ing("Basilico", "qualche foglia"),
                ing("Olio extravergine d'oliva", "2 cucchiai"), basic("Origano", "q.b."));

        recipe("Spinaci saltati all'aglio", "Contorno pronto in dieci minuti.",
                Diet.VEGANA, 10, 2,
                List.of("Lava bene gli spinaci.",
                        "Scalda l'olio con l'aglio in camicia.",
                        "Aggiungi gli spinaci e cuocili 4 minuti mescolando.",
                        "Togli l'aglio e completa con qualche goccia di limone."),
                ing("Spinaci", "400 g"), ing("Aglio", "2 spicchi"), ing("Limoni", "1/2"),
                ing("Olio extravergine d'oliva", "2 cucchiai"), basic("Sale", "q.b."));

        recipe("Torta di mele", "Il dolce di casa per le mele un po' ammaccate.",
                Diet.VEGETARIANA, 60, 8,
                List.of("Scalda il forno a 180 gradi e imburra una tortiera.",
                        "Monta le uova con lo zucchero, poi aggiungi il burro fuso e il latte.",
                        "Incorpora la farina setacciata con il lievito.",
                        "Versa nella tortiera e copri con le mele tagliate a fettine.",
                        "Cuoci per 40 minuti e fai la prova stecchino."),
                ing("Mele", "3"), ing("Farina 00", "250 g"), ing("Uova", "3"), ing("Burro", "100 g"),
                ing("Latte intero", "100 ml"), basic("Zucchero", "150 g"), basic("Lievito per dolci", "1 bustina"));

        recipe("Penne panna, prosciutto e piselli", "Primo piatto veloce e amato da tutti.",
                Diet.ONNIVORA, 20, 4,
                List.of("Cuoci i piselli in padella con un filo d'olio per 8 minuti.",
                        "Aggiungi il prosciutto a dadini e la panna.",
                        "Cuoci le penne, scolale e uniscile al condimento.",
                        "Completa con il parmigiano."),
                ing("Penne", "320 g"), ing("Prosciutto cotto", "150 g"), ing("Panna da cucina", "200 ml"),
                ing("Piselli surgelati", "150 g"), ing("Parmigiano Reggiano", "30 g"));

        recipe("Parmigiana di melanzane veloce", "Versione più leggera, con le melanzane grigliate.",
                Diet.VEGETARIANA, 50, 4,
                List.of("Taglia le melanzane a fette e grigliale.",
                        "Cuoci la passata con olio, sale e basilico per 10 minuti.",
                        "In una pirofila alterna sugo, melanzane, mozzarella e parmigiano.",
                        "Cuoci in forno a 200 gradi per 25 minuti."),
                ing("Melanzane", "2"), ing("Passata di pomodoro", "400 g"), ing("Mozzarella", "200 g"),
                ing("Parmigiano Reggiano", "50 g"), ing("Basilico", "qualche foglia"),
                ing("Olio extravergine d'oliva", "2 cucchiai"));

        recipe("Crema di carote", "Vellutata dolce e leggera.",
                Diet.VEGANA, 35, 4,
                List.of("Pela carote e patata e tagliale a pezzi.",
                        "Fai appassire la cipolla nell'olio.",
                        "Aggiungi le verdure, copri con acqua e cuoci 25 minuti.",
                        "Frulla tutto con un frullatore a immersione e regola di sale."),
                ing("Carote", "500 g"), ing("Patate", "1"), ing("Cipolle", "1"),
                ing("Olio extravergine d'oliva", "2 cucchiai"), basic("Sale", "q.b."));
    }

    private record Ing(String productName, String name, String amount) {
    }

    /** Ingrediente collegato a un prodotto del catalogo. */
    private Ing ing(String productName, String amount) {
        return new Ing(productName, productName, amount);
    }

    /** Ingrediente di base non presente nel catalogo (sale, acqua...). */
    private Ing basic(String name, String amount) {
        return new Ing(null, name, amount);
    }

    private void recipe(String name, String description, Diet diet, int minutes, int servings,
                        List<String> steps, Ing... ingredients) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setDescription(description);
        recipe.setDiet(diet);
        recipe.setPrepMinutes(minutes);
        recipe.setServings(servings);
        recipe.setInstructions(String.join("\n", steps));
        for (Ing i : ingredients) {
            Product product = i.productName() == null ? null : products.get(i.productName());
            recipe.addIngredient(new RecipeIngredient(product, i.name(), i.amount()));
        }
        recipeRepository.save(recipe);
    }

    // ------------------------------------------------------------------ utenti

    private void loadUsers() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@smartpantry.it");
        admin.setPassword(passwordEncoder.encode("admin1234"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User demo = new User();
        demo.setUsername("demo");
        demo.setEmail("demo@smartpantry.it");
        demo.setPassword(passwordEncoder.encode("demo1234"));
        demo.setRole(Role.USER);
        demo.setFoodPreferences("Piatti veloci, poco piccanti");
        userRepository.save(demo);

        LocalDate today = LocalDate.now();
        // già scaduti (rossi)
        item(demo, "Latte intero", 1, today.minusDays(5), today.minusDays(1), Storage.FRIGO);
        item(demo, "Fragole", 1, today.minusDays(4), today.minusDays(2), Storage.FRIGO);
        // in scadenza entro 3 giorni (arancioni)
        item(demo, "Mozzarella", 1, today.minusDays(4), today, Storage.FRIGO);
        item(demo, "Zucchine", 3, today.minusDays(4), today.plusDays(1), Storage.FRIGO);
        item(demo, "Uova", 6, today.minusDays(18), today.plusDays(2), Storage.FRIGO);
        item(demo, "Petto di pollo", 1, today.minusDays(1), today.plusDays(2), Storage.FRIGO);
        item(demo, "Basilico", 1, today.minusDays(2), today.plusDays(3), Storage.DISPENSA);
        // a posto (verdi)
        item(demo, "Banane", 3, today.minusDays(1), today.plusDays(4), Storage.DISPENSA);
        item(demo, "Pomodori", 4, today.minusDays(1), today.plusDays(6), Storage.DISPENSA);
        item(demo, "Patate", 5, today.minusDays(10), today.plusDays(20), Storage.DISPENSA);
        item(demo, "Cipolle", 3, today.minusDays(5), today.plusDays(25), Storage.DISPENSA);
        item(demo, "Parmigiano Reggiano", 1, today.minusDays(10), today.plusDays(50), Storage.FRIGO);
        item(demo, "Spaghetti", 2, today.minusDays(30), today.plusDays(300), Storage.DISPENSA);
        item(demo, "Passata di pomodoro", 2, today.minusDays(30), today.plusDays(200), Storage.DISPENSA);
        item(demo, "Olio extravergine d'oliva", 1, today.minusDays(40), today.plusDays(400), Storage.DISPENSA);
        item(demo, "Piselli surgelati", 1, today.minusDays(20), today.plusDays(250), Storage.FREEZER);

        // soglie minime: latte e spaghetti sono sotto soglia e finiranno nella lista
        threshold(demo, "Latte intero", 2);
        threshold(demo, "Spaghetti", 3);
        threshold(demo, "Uova", 6);
        threshold(demo, "Olio extravergine d'oliva", 1);

        // una voce aggiunta a mano
        ShoppingListItem apples = new ShoppingListItem();
        apples.setUser(demo);
        apples.setProduct(products.get("Mele"));
        apples.setTargetQuantity(6);
        shoppingListRepository.save(apples);

        // storico dell'ultimo mese per le statistiche della dashboard
        movement(demo, "Latte intero", MovementType.CONSUMATO, 3, 20);
        movement(demo, "Pane", MovementType.CONSUMATO, 4, 15);
        movement(demo, "Pane", MovementType.BUTTATO, 1, 14);
        movement(demo, "Yogurt bianco", MovementType.CONSUMATO, 4, 10);
        movement(demo, "Insalata", MovementType.BUTTATO, 1, 6);
        movement(demo, "Mele", MovementType.CONSUMATO, 5, 3);
        movement(demo, "Spaghetti", MovementType.CONSUMATO, 1, 2);

        pantryItemRepository.flush();
        shoppingListService.refreshAll(demo.getId());
    }

    private void item(User user, String productName, int quantity, LocalDate purchase, LocalDate expiry, Storage storage) {
        PantryItem item = new PantryItem();
        item.setUser(user);
        item.setProduct(products.get(productName));
        item.setQuantity(quantity);
        item.setPurchaseDate(purchase);
        item.setExpirationDate(expiry);
        item.setStorage(storage);
        pantryItemRepository.save(item);
    }

    private void threshold(User user, String productName, int min) {
        StockThreshold threshold = new StockThreshold();
        threshold.setUser(user);
        threshold.setProduct(products.get(productName));
        threshold.setMinQuantity(min);
        thresholdRepository.save(threshold);
    }

    private void movement(User user, String productName, MovementType type, int quantity, int daysAgo) {
        StockMovement movement = new StockMovement(user, products.get(productName), type, quantity);
        movement.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        movementRepository.save(movement);
    }

    public DataLoader(CategoryRepository categoryRepository, ProductRepository productRepository, RecipeRepository recipeRepository, UserRepository userRepository, PantryItemRepository pantryItemRepository, StockThresholdRepository thresholdRepository, StockMovementRepository movementRepository, ShoppingListRepository shoppingListRepository, ShoppingListService shoppingListService, PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.thresholdRepository = thresholdRepository;
        this.movementRepository = movementRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.shoppingListService = shoppingListService;
        this.passwordEncoder = passwordEncoder;
    }
}
