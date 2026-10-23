-- =====================================================================
--  Smart Pantry - script DDL del database smart_pantry_db
--  Viene eseguito in automatico all'avvio dell'applicazione.
--  Si può anche eseguire a mano in MySQL Workbench: prima eseguire
--      CREATE DATABASE IF NOT EXISTS smart_pantry_db;
--      USE smart_pantry_db;
-- =====================================================================

-- ---------- utenti ----------
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    username         VARCHAR(50)  NOT NULL,
    email            VARCHAR(120) NOT NULL,
    password         VARCHAR(100) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    diet             VARCHAR(20)  NOT NULL DEFAULT 'ONNIVORA',
    food_preferences VARCHAR(500) NULL,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;

-- allergie dichiarate nel profilo (una riga per allergene)
CREATE TABLE IF NOT EXISTS user_allergens (
    user_id  BIGINT      NOT NULL,
    allergen VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, allergen),
    CONSTRAINT fk_user_allergens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------- catalogo (gestito dall'admin) ----------
CREATE TABLE IF NOT EXISTS categories (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    barcode         VARCHAR(32)  NULL,
    category_id     BIGINT       NOT NULL,
    shelf_life_days INT          NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_name UNIQUE (name),
    CONSTRAINT uk_products_barcode UNIQUE (barcode),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB;

-- allergeni contenuti in un prodotto
CREATE TABLE IF NOT EXISTS product_allergens (
    product_id BIGINT      NOT NULL,
    allergen   VARCHAR(30) NOT NULL,
    PRIMARY KEY (product_id, allergen),
    CONSTRAINT fk_product_allergens_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------- dispensa ----------
CREATE TABLE IF NOT EXISTS pantry_items (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    product_id      BIGINT      NOT NULL,
    quantity        INT         NOT NULL,
    expiration_date DATE        NOT NULL,
    purchase_date   DATE        NOT NULL,
    storage         VARCHAR(20) NOT NULL DEFAULT 'DISPENSA',
    PRIMARY KEY (id),
    CONSTRAINT fk_pantry_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_pantry_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_pantry_items_quantity CHECK (quantity >= 0)
) ENGINE = InnoDB;

-- soglia minima impostata dall'utente per un prodotto
CREATE TABLE IF NOT EXISTS stock_thresholds (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    user_id      BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    min_quantity INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_thresholds UNIQUE (user_id, product_id),
    CONSTRAINT fk_stock_thresholds_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_thresholds_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_stock_thresholds_min CHECK (min_quantity >= 1)
) ENGINE = InnoDB;

-- storico dei movimenti: acquistato / consumato / buttato
CREATE TABLE IF NOT EXISTS stock_movements (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    type       VARCHAR(20) NOT NULL,
    quantity   INT         NOT NULL,
    created_at DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB;

-- ---------- lista della spesa ----------
CREATE TABLE IF NOT EXISTS shopping_list (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    user_id         BIGINT   NOT NULL,
    product_id      BIGINT   NOT NULL,
    target_quantity INT      NOT NULL,
    is_bought       BOOLEAN  NOT NULL DEFAULT FALSE,
    auto_generated  BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at      DATETIME NOT NULL,
    bought_at       DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_shopping_list_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_list_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_shopping_list_quantity CHECK (target_quantity >= 1)
) ENGINE = InnoDB;

-- ---------- ricette ----------
CREATE TABLE IF NOT EXISTS recipes (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(150) NOT NULL,
    description  VARCHAR(500) NULL,
    instructions TEXT         NOT NULL,
    prep_minutes INT          NOT NULL,
    servings     INT          NOT NULL,
    diet         VARCHAR(20)  NOT NULL DEFAULT 'ONNIVORA',
    ai_generated BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by   BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recipes_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    recipe_id       BIGINT       NOT NULL,
    product_id      BIGINT       NULL,
    ingredient_name VARCHAR(100) NOT NULL,
    amount          VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recipe_ingredients_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_ingredients_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL
) ENGINE = InnoDB;
