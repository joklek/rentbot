CREATE TABLE user_districts
(
    user_id     INTEGER NOT NULL,
    district_id INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (telegram_id),
    FOREIGN KEY (district_id) REFERENCES districts (id)
);