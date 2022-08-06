CREATE TABLE users
(
    id                 INTEGER        NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    telegram_id        INTEGER        NOT NULL,
    enabled            BOOL           NOT NULL DEFAULT 0,
    price_min          DECIMAL(19, 4) NULL,
    price_max          DECIMAL(19, 4) NULL,
    rooms_min          INTEGER        NULL,
    rooms_max          INTEGER        NULL,
    year_min           INTEGER        NULL,
    floor_min          INTEGER        NULL,
    show_with_fees     BOOL           NOT NULL DEFAULT 0,
    filter_by_district BOOL           NOT NULL DEFAULT 0
);

CREATE TABLE posts
(
    id                INTEGER        NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    external_id       VARCHAR(63)    NOT NULL,
    source            VARCHAR(63)    NOT NULL,
    link              VARCHAR(1023)  NOT NULL,
    last_seen         TIMESTAMP      NOT NULL,
    is_with_fees      BOOL           NOT NULL,
    phone             VARCHAR(63)    NULL,
    price             DECIMAL(19, 4) NULL,
    rooms             INTEGER        NULL,
    construction_year INTEGER        NULL,
    floor             INTEGER        NULL,
    total_floors      INTEGER        NULL,
    street            VARCHAR(63)    NULL,
    district          VARCHAR(63)    NULL,
    house_number      VARCHAR(63)    NULL,
    heating           VARCHAR(63)    NULL,
    area              DECIMAL(8, 2)  NULL,
    description_hash  VARCHAR(125)   NULL
);

CREATE TABLE districts
(
    id   INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    name VARCHAR(125) NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_posts_external_id_source ON posts (external_id, source);
CREATE UNIQUE INDEX IF NOT EXISTS users_telegram_id ON users (telegram_id);