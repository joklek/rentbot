CREATE TABLE `users`
(
    id                 INTEGER        NOT NULL,
    enabled            BOOL           NOT NULL,
    price_min          DECIMAL(19, 4) NOT NULL,
    price_max          DECIMAL(19, 4) NOT NULL,
    rooms_min          INTEGER        NOT NULL,
    rooms_max          INTEGER        NOT NULL,
    year_min           INTEGER        NOT NULL,
    floor_min          INTEGER        NOT NULL,
    show_with_fees     BOOL           NOT NULL,
    filter_by_district BOOL           NOT NULL,
    CONSTRAINT PK__users PRIMARY KEY (id)
);

CREATE TABLE `posts`
(
    id                INTEGER AUTO_INCREMENT,
    external_id       VARCHAR(63)    NOT NULL,
    source            VARCHAR(63)    NOT NULL,
    link              VARCHAR(1023)  NOT NULL,
    phone             VARCHAR(63)    NOT NULL,
    last_seen         TIMESTAMP      NOT NULL,
    price             DECIMAL(19, 4) NOT NULL,
    rooms             INTEGER        NOT NULL,
    construction_year INTEGER        NOT NULL,
    floor             INTEGER        NOT NULL,
    total_floors      INTEGER        NOT NULL,
    is_with_fees      BOOL           NOT NULL,
    street            VARCHAR(63)    NULL,
    district          VARCHAR(63)    NULL,
    house_number      VARCHAR(63)    NULL,
    heating           VARCHAR(63)    NULL,
    area              DECIMAL(19, 4) NULL,
    description_hash  VARCHAR(125)   NULL,
    CONSTRAINT PK__posts PRIMARY KEY (id)
);

CREATE TABLE `districts`
(
    id   INTEGER AUTO_INCREMENT,
    name VARCHAR(125) NULL,
    CONSTRAINT PK__districts PRIMARY KEY (id)
);