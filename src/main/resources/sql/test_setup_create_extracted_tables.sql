ALTER TABLE `gcd_story_credit` ADD `issue_id` INT(11) DEFAULT NULL AFTER `story_id`;
ALTER TABLE `gcd_story_credit` ADD `series_id` INT(11) DEFAULT NULL AFTER `issue_id`;
ALTER TABLE `gcd_story_credit` ADD FOREIGN KEY (`issue_id`) REFERENCES `gcd_issue`(`id`);
ALTER TABLE `gcd_story_credit` ADD FOREIGN KEY (`series_id`) REFERENCES `gcd_series`(`id`);

CREATE TABLE IF NOT EXISTS m_character (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    alter_ego VARCHAR(255),
    publisher_id INTEGER NOT NULL REFERENCES gcd_publisher (id)
);

CREATE TABLE IF NOT EXISTS m_character_appearance (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    details VARCHAR(255),
    character_id INTEGER NOT NULL,
    story_id INTEGER NOT NULL,
    notes VARCHAR(255),
    membership LONGTEXT,
    issue_id INTEGER DEFAULT NULL,
    series_id INTEGER DEFAULT NULL,
    FOREIGN KEY (character_id) REFERENCES m_character (id),
    FOREIGN KEY (story_id) REFERENCES gcd_story (id),
    FOREIGN KEY (issue_id) REFERENCES gcd_issue (id),
    FOREIGN KEY (series_id) REFERENCES gcd_series (id)
);

CREATE TABLE IF NOT EXISTS `m_story_credit` (
    `id` int (11) NOT NULL AUTO_INCREMENT,
    `creator_id` int (11) NOT NULL,
    `credit_type_id` int (11) NOT NULL,
    `story_id` int (11) NOT NULL,
    `issue_id` int (11) DEFAULT NULL,
    `series_id` int (11) DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`creator_id`) REFERENCES `gcd_creator_name_detail`(`id`),
    FOREIGN KEY (`credit_type_id`) REFERENCES `gcd_credit_type`(`id`),
    FOREIGN KEY (`story_id`) REFERENCES `gcd_story`(`id`),
    FOREIGN KEY (`issue_id`) REFERENCES `gcd_issue`(`id`),
    FOREIGN KEY (`series_id`) REFERENCES `gcd_series`(`id`)
);

DROP TABLE IF EXISTS {{sourceSchema}}.good_publishers;

CREATE TABLE {{sourceSchema}}.good_publishers AS
SELECT DISTINCT gp.*
FROM {{sourceSchema}}.gcd_publisher gp
JOIN {{sourceSchema}}.gcd_series gs ON gp.id = gs.publisher_id
WHERE gp.country_id = 225
  AND  gs.year_began >= 1900;

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_publishers;

CREATE TABLE {{sourceSchema}}.migrate_publishers AS
SELECT new.*
FROM {{sourceSchema}}.good_publishers new
  LEFT JOIN {{targetSchema}}.gcd_publisher old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_publisher
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS {{sourceSchema}}.good_indicia_publishers;

CREATE TABLE {{sourceSchema}}.good_indicia_publishers AS
SELECT gip.*
FROM {{sourceSchema}}.gcd_indicia_publisher gip
WHERE gip.parent_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_publishers
  );

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_indicia_publishers;

CREATE TABLE {{sourceSchema}}.migrate_indicia_publishers AS
SELECT new.*
FROM {{sourceSchema}}.good_indicia_publishers new
  LEFT JOIN {{targetSchema}}.gcd_indicia_publisher old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_indicia_publisher
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS {{sourceSchema}}.good_series;

CREATE TABLE {{sourceSchema}}.good_series AS
SELECT *
FROM {{sourceSchema}}.gcd_series
WHERE publisher_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_publishers
  )
  AND country_id = 225
  AND language_id = 25
  AND year_began >= 1900;

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_series;

CREATE TABLE {{sourceSchema}}.migrate_series AS
SELECT new.*
FROM {{sourceSchema}}.good_series new
  LEFT JOIN {{targetSchema}}.gcd_series old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_series
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS {{sourceSchema}}.good_issue;

CREATE TABLE {{sourceSchema}}.good_issue AS
SELECT *
FROM {{sourceSchema}}.gcd_issue
WHERE series_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_series
  )
AND (indicia_publisher_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_indicia_publishers
    )
OR indicia_publisher_id IS NULL);

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_issues;

CREATE TABLE {{sourceSchema}}.migrate_issues AS
SELECT new.*
FROM {{sourceSchema}}.good_issue new
  LEFT JOIN {{targetSchema}}.gcd_issue old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_issue
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS {{sourceSchema}}.good_story;

CREATE TABLE {{sourceSchema}}.good_story AS
SELECT *
FROM {{sourceSchema}}.gcd_story
WHERE issue_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_issue
  )
  AND type_id IN (6, 19);

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_stories;

CREATE TABLE {{sourceSchema}}.migrate_stories AS
SELECT new.*
FROM {{sourceSchema}}.good_story new
  LEFT JOIN {{targetSchema}}.gcd_story old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_story
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS {{sourceSchema}}.good_story_credit;

CREATE TABLE {{sourceSchema}}.good_story_credit AS
SELECT *
FROM {{sourceSchema}}.gcd_story_credit
WHERE story_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_story
  );

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_story_credits;

CREATE TABLE {{sourceSchema}}.migrate_story_credits AS
SELECT new.*
FROM {{sourceSchema}}.good_story_credit new
  LEFT JOIN {{targetSchema}}.gcd_story_credit old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_story_credit
  )
  OR new.modified != old.modified;
