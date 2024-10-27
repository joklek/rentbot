CREATE TABLE post_price_history
(
    id         INTEGER        NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    post_id    INTEGER        NOT NULL,
    price      DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP      NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts (id)
);

INSERT INTO post_price_history (post_id, price, created_at)
SELECT id, price, created_at
FROM posts
WHERE price IS NOT NULL;
