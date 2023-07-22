DROP TABLE IF EXISTS {{targetSchema}}.stddata_country;
DROP TABLE IF EXISTS {{targetSchema}}.stddata_language;
DROP TABLE IF EXISTS {{targetSchema}}.stddata_date;
DROP TABLE IF EXISTS {{targetSchema}}.gcd_series_publication_type;
DROP TABLE IF EXISTS {{targetSchema}}.gcd_brand;
DROP TABLE IF EXISTS {{targetSchema}}.gcd_story_type;
DROP TABLE IF EXISTS {{targetSchema}}.gcd_name_type;
DROP TABLE IF EXISTS {{targetSchema}}.stddata_script;
DROP TABLE IF EXISTS {{targetSchema}}.gcd_creator_signature;

CREATE TABLE IF NOT EXISTS {{targetSchema}}.stddata_country (
    id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.stddata_language (
    id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    native_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.stddata_date (
    id INT NOT NULL AUTO_INCREMENT,
    year INT NOT NULL,
    month INT NOT NULL,
    day INT NOT NULL,
    year_uncertain TINYINT(1) NOT NULL DEFAULT 0,
    month_uncertain TINYINT(1) NOT NULL DEFAULT 0,
    day_uncertain TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.gcd_series_publication_type (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    notes VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.gcd_brand (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    modified DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.gcd_story_type (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    sort_code INT(11) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.gcd_name_type (
    id INT NOT NULL AUTO_INCREMENT,
    description longtext DEFAULT NULL,
    type VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.stddata_script (
    id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(4) NOT NULL,
    number smallint(5) NOT NULL,
    name VARCHAR(64) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS {{targetSchema}}.gcd_creator_signature (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    modified DATETIME NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO {{targetSchema}}.stddata_country (id, code, name)
VALUES (225, 'US', 'United States'),
       (36, 'CA', 'Canada'),
       (150, 'MX', 'Mexico');

INSERT INTO {{targetSchema}}.stddata_language (id, code, name, native_name)
VALUES (25, 'en', 'English', 'English'),
       (34, 'fr', 'French', 'Français'),
       (27, 'es', 'Spanish', 'Español');

INSERT INTO {{targetSchema}}.stddata_date (id, year, month, day, year_uncertain, month_uncertain, day_uncertain)
VALUES (1, 2019, 1, 1, 0, 0, 0),
       (2, 2019, 1, 2, 0, 0, 0),
       (3, 2019, 1, 3, 0, 0, 0);

INSERT INTO {{targetSchema}}.gcd_series_publication_type (id, name, notes)
VALUES (1, 'book', NULL),
    (2, 'magazine', NULL),
    (3, 'album', NULL);

INSERT INTO {{targetSchema}}.gcd_brand (id, name, modified)
VALUES (1, 'DC', '2019-01-01 00:00:00'),
    (2, 'Marvel', '2019-01-01 00:00:00'),
    (3, 'Image', '2019-01-01 00:00:00');

INSERT INTO {{targetSchema}}.gcd_story_type (id, name, sort_code)
VALUES (1, 'cover', 1),
    (2, 'story', 2),
    (3, 'promo', 3);

INSERT INTO {{targetSchema}}.gcd_name_type (id, description, type)
VALUES (1, NULL, 'Changed Name'),
 (2, 'Native Language', 'Native Language (type is deprecated)'),
 (3, NULL, 'Name at Birth');

INSERT INTO {{targetSchema}}.stddata_script (id, code, number, name)
VALUES (1, 'Latn', 215, 'Latin'),
    (2, 'Cyrl', 220, 'Cyrillic'),
    (3, 'Grek', 200, 'Greek');

INSERT INTO {{targetSchema}}.gcd_creator_signature (id, name, modified)
VALUES (1, 'John Smith', '2019-01-01 00:00:00'),
    (2, 'Jane Doe', '2019-01-01 00:00:00'),
    (3, 'John Doe', '2019-01-01 00:00:00');