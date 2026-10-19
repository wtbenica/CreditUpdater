DROP TABLE IF EXISTS `{{targetSchema}}`.merge_key_work;
DROP TABLE IF EXISTS `{{targetSchema}}`.merge_best_work;
DROP TABLE IF EXISTS `{{targetSchema}}`.merge_duplicate_work;
CREATE TABLE `{{targetSchema}}`.merge_key_work
(
    id           INT NOT NULL,
    publisher_id INT NULL,
    name_key     VARCHAR(255) CHARACTER SET utf8mb4 NULL,
    alter_key    VARCHAR(255) CHARACTER SET utf8mb4 NULL,
    linked       TINYINT NOT NULL,
    uses         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX (publisher_id, name_key, alter_key)
);
INSERT INTO `{{targetSchema}}`.merge_key_work (id, publisher_id, name_key, alter_key, linked)
SELECT m.id,
       m.publisher_id,
       LEFT(LOWER(REGEXP_REPLACE(m.name, '[^[:alnum:]]+', '')), 255),
       LEFT(LOWER(REGEXP_REPLACE(IFNULL(m.alter_ego, ''), '[^[:alnum:]]+', '')), 255),
       IF(m.gcd_character_id IS NULL, 0, 1)
FROM `{{targetSchema}}`.m_character m;
UPDATE `{{targetSchema}}`.merge_key_work k
    JOIN (SELECT character_id, COUNT(*) AS n
          FROM `{{targetSchema}}`.m_character_appearance
          GROUP BY character_id) u ON u.character_id = k.id
SET k.uses = u.n;
ALTER TABLE `{{targetSchema}}`.merge_key_work ADD COLUMN score BIGINT NOT NULL DEFAULT 0;
UPDATE `{{targetSchema}}`.merge_key_work
SET score = linked * 1000000000000000 + uses * 100000000 + (99999999 - id);
CREATE TABLE `{{targetSchema}}`.merge_best_work AS
SELECT publisher_id, name_key, alter_key, 99999999 - (MAX(score) MOD 100000000) AS keep_id
FROM `{{targetSchema}}`.merge_key_work
WHERE name_key <> ''
GROUP BY publisher_id, name_key, alter_key
HAVING COUNT(*) > 1;
ALTER TABLE `{{targetSchema}}`.merge_best_work ADD INDEX (publisher_id, name_key, alter_key);
CREATE TABLE `{{targetSchema}}`.merge_duplicate_work AS
SELECT k.id AS duplicate_id, b.keep_id
FROM `{{targetSchema}}`.merge_key_work k
         JOIN `{{targetSchema}}`.merge_best_work b
              ON b.publisher_id = k.publisher_id AND b.name_key = k.name_key AND b.alter_key = k.alter_key
WHERE k.id <> b.keep_id
  AND k.linked = 0;
ALTER TABLE `{{targetSchema}}`.merge_duplicate_work ADD PRIMARY KEY (duplicate_id), ADD INDEX (keep_id);
UPDATE `{{targetSchema}}`.m_character_appearance a
    JOIN `{{targetSchema}}`.merge_duplicate_work d ON d.duplicate_id = a.character_id
SET a.character_id = d.keep_id;
DELETE a
FROM `{{targetSchema}}`.m_character_appearance a
         JOIN `{{targetSchema}}`.m_character_appearance b
              ON b.story_id = a.story_id AND b.character_id = a.character_id AND b.id < a.id
WHERE a.character_id IN (SELECT keep_id FROM `{{targetSchema}}`.merge_duplicate_work);
DELETE m
FROM `{{targetSchema}}`.m_character m
         JOIN `{{targetSchema}}`.merge_duplicate_work d ON d.duplicate_id = m.id;
DROP TABLE `{{targetSchema}}`.merge_key_work;
DROP TABLE `{{targetSchema}}`.merge_best_work;
DROP TABLE `{{targetSchema}}`.merge_duplicate_work;
CREATE TABLE `{{targetSchema}}`.merge_key_work
(
    id           INT NOT NULL,
    publisher_id INT NULL,
    name_key     VARCHAR(255) CHARACTER SET utf8mb4 NULL,
    alter_key    VARCHAR(255) CHARACTER SET utf8mb4 NULL,
    linked       TINYINT NOT NULL,
    uses         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX (publisher_id, name_key, alter_key)
);
INSERT INTO `{{targetSchema}}`.merge_key_work (id, publisher_id, name_key, alter_key, linked)
SELECT m.id,
       m.publisher_id,
       LEFT(LOWER(REGEXP_REPLACE(m.name, '[^[:alnum:]]+', '')), 255),
       LEFT(LOWER(REGEXP_REPLACE(IFNULL(m.alter_ego, ''), '[^[:alnum:]]+', '')), 255),
       IF(m.gcd_character_id IS NULL, 0, 1)
FROM `{{targetSchema}}`.m_character m;
UPDATE `{{targetSchema}}`.merge_key_work k
    JOIN (SELECT character_id, COUNT(*) AS n
          FROM `{{targetSchema}}`.m_character_appearance
          GROUP BY character_id) u ON u.character_id = k.id
SET k.uses = u.n;
CREATE TABLE `{{targetSchema}}`.merge_duplicate_work AS
SELECT IF(a.linked > b.linked OR (a.linked = b.linked AND (a.uses > b.uses OR (a.uses = b.uses AND a.id < b.id))),
          b.id, a.id) AS duplicate_id,
       IF(a.linked > b.linked OR (a.linked = b.linked AND (a.uses > b.uses OR (a.uses = b.uses AND a.id < b.id))),
          a.id, b.id) AS keep_id
FROM `{{targetSchema}}`.merge_key_work a
         JOIN `{{targetSchema}}`.merge_key_work b
              ON b.publisher_id = a.publisher_id AND b.name_key = a.alter_key AND b.alter_key = a.name_key
WHERE a.id < b.id
  AND a.name_key <> ''
  AND a.alter_key <> ''
  AND a.name_key <> a.alter_key
  AND NOT (a.linked = 1 AND b.linked = 1);
ALTER TABLE `{{targetSchema}}`.merge_duplicate_work ADD INDEX (duplicate_id), ADD INDEX (keep_id);
UPDATE `{{targetSchema}}`.m_character_appearance a
    JOIN `{{targetSchema}}`.merge_duplicate_work d ON d.duplicate_id = a.character_id
SET a.character_id = d.keep_id;
DELETE a
FROM `{{targetSchema}}`.m_character_appearance a
         JOIN `{{targetSchema}}`.m_character_appearance b
              ON b.story_id = a.story_id AND b.character_id = a.character_id AND b.id < a.id
WHERE a.character_id IN (SELECT keep_id FROM `{{targetSchema}}`.merge_duplicate_work);
DELETE m
FROM `{{targetSchema}}`.m_character m
         JOIN `{{targetSchema}}`.merge_duplicate_work d ON d.duplicate_id = m.id;
DROP TABLE `{{targetSchema}}`.merge_key_work;
DROP TABLE `{{targetSchema}}`.merge_duplicate_work;
