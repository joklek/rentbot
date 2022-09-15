INSERT INTO districts(name)
VALUES ('Bajorai'),
       ('Tarandė');

DELETE
FROM districts
WHERE name IN ('Pavilnys',
               'Saulėtekis',
               'Filaretai',
               'Buivydiškės',
               'Bukčiai');