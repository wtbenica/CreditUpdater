INSERT IGNORE INTO {{targetSchema}}.stddata_country (id, code, name, modified)
VALUES (14, 'AU', 'Australia', '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.stddata_language (id, code, name, native_name, modified)
VALUES (22, 'de', 'German', 'Deutsch', '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.stddata_date (id, year, month, day, year_uncertain, month_uncertain, day_uncertain, modified)
VALUES (4, 2000, 1, 1, 0, 0, 0, '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.gcd_series_publication_type (id, name, notes, modified)
VALUES (4, 'limited series', 'A limited series is a comic book series with a set number of issues.', '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.gcd_brand (id, name, modified)
VALUES (4, 'Vertigo', '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.gcd_story_type (id, name, sort_code, modified)
VALUES (4, 'letters column', 4, '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.gcd_name_type (id, description, type, modified)
VALUES (4, 'This is a pseudonym.', 'pseudonym', '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.stddata_script (id, code, number, name, modified)
VALUES (7, 'Beng', 325, 'Bengali', '2023-06-01 19:56:37');

INSERT IGNORE INTO {{targetSchema}}.gcd_creator_signature (id, name, modified)
VALUES (4, 'Grant Morrison', '2023-06-01 19:56:37');

INSERT IGNORE INTO `{{targetSchema}}`.`gcd_publisher` (`id`, `name`, `country_id`, `year_began`, `modified`)
VALUES (6, 'GOOD New publisher', 225, 2000, '2023-06-01 19:56:37'),
(7, 'BAD country_id', 14, 2000, '2023-06-01 19:56:37');

INSERT IGNORE INTO `{{targetSchema}}`.`gcd_series` (
        `id`,
        `name`,
        `year_began`,
        `publisher_id`,
        `country_id`,
        `language_id`,
        `first_issue_id`,
        `last_issue_id`,
        `modified`
    )
VALUES
    (11, 'GOOD New series existing publisher',  2000, 1, 225, 25, 1, 2, '2023-06-01 19:56:37'),
    (12, 'GOOD New series new publisher',       2000, 6, 225, 25, 1, 2, '2023-06-01 19:56:37'),
    (13, 'BAD publisher_id',                    2000, 3, 225, 25, 1, 2, '2023-06-01 19:56:37'),
    (14, 'BAD country_id',                      2000, 1, 14, 25, 1, 2, '2023-06-01 19:56:37'),
    (15, 'BAD language_id',                     2000, 1, 225, 22, 1, 2, '2023-06-01 19:56:37');

INSERT IGNORE INTO `{{targetSchema}}`.`gcd_indicia_publisher` (`id`, `name`, `parent_id`, `modified`)
VALUES (3, 'BAD parent_id', 3, '2023-06-01 19:56:37'),
    (4, 'BAD parent_id', 4, '2023-06-01 19:56:37'),
    (5, 'GOOD existing publisher', 1, '2023-06-01 19:56:37'),
    (6, 'GOOD new publisher', 6, '2023-06-01 19:56:37'),
    (7, 'BAD parent_id', 4, '2023-06-01 19:56:37');

# 9 - good - existing series;
# 10 - good - new series;
# 11 - bad - bad series;
# 12 - bad - bad indicia_publisher_id;
INSERT IGNORE INTO `{{targetSchema}}`.`gcd_issue` (`id`, `number`, `series_id`, `indicia_publisher_id`, `modified`)
VALUES (9, 11, 1, 1, '2023-06-01 19:56:37'),
    (10, 12, 11, 6, '2023-06-01 19:56:37'),
    (11, 13, 4, 1, '2023-06-01 19:56:37'),
    (12, 14, 7, 3, '2023-06-01 19:56:37');

# 8 - good old series origin, good new series target;
INSERT IGNORE INTO `{{targetSchema}}`.`gcd_series_bond` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`, `modified`)
VALUES (8, 1, 9, 1, 2, '2023-06-01 19:56:37');

INSERT IGNORE INTO `{{targetSchema}}`.`gcd_creator` (`id`, `name`, `modified`)
VALUES (9, 'Neil Gaiman', '2023-06-01 19:56:37'),
(10, 'Jonathan Hickman', '2023-06-01 19:56:37');

INSERT IGNORE INTO `{{targetSchema}}`.`gcd_creator_name_detail` (`id`, `name`, `creator_id`, `modified`)
VALUES (9, 'Grant T. Morrison', 1, '2023-06-01 19:56:37'),
    (10, 'Neil Richard Gaiman', 9, '2023-06-01 19:56:37'),
    (11, 'Jonathan Hickman', 10, '2023-06-01 19:56:37');

INSERT IGNORE INTO `{{targetSchema}}`.`gcd_story` (
        `id`,
        `title`,
        `issue_id`,
        `script`,
        `pencils`,
        `inks`,
        `colors`,
        `letters`,
        `editing`,
        `characters`,
        `modified`
    )
VALUES (
        8,
        'GOOD issue 1',
        1,
        'Grant Morrison',
        'Richard Case',
        'John Nyberg',
        'Daniel Vozzo',
        'John Workman',
        'Art Young',
        'Individual [Alter Ego] (detail); Team [Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)]',
        '2023-06-01 19:56:37'
    ),
    (
        9,
        'GOOD issue 9',
        9,
        'Neil Gaiman',
        'Chris Bachalo',
        'Mark Buckingham',
        'Steve Oliff',
        'Todd Klein',
        'Karen Berger',
        'Individual [Alter Ego] (detail); Team [Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)]',
        '2023-06-01 19:56:37'
    ),
    (
        10,
        'BAD issue 3',
        3,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        '2023-06-01 19:56:37'
    ),
    (
        11,
        'BAD issue 11',
        11,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        '2023-06-01 19:56:37'
    );

INSERT IGNORE INTO `{{targetSchema}}`.`gcd_story_credit` (`id`, `creator_id`, `credit_type_id`, `story_id`, `modified`)
VALUES (11, 4, 1, 8, '2023-06-01 19:56:37'),
    (12, 3, 5, 9, '2023-06-01 19:56:37'),
    (13, 3, 4, 10, '2023-06-01 19:56:37'),
    (14, 3, 6, 11, '2023-06-01 19:56:37');

# 8 - GOOD new target id;
# 9 - BAD bad origin id;
# 10 - BAD bad new target id;
# 11 - BAD good new origin and target ids and bad issue ids;
# 12 - GOOD new issue ids;
# 13 - BAD bad new issue ids;
INSERT IGNORE INTO `{{targetSchema}}`.`gcd_reprint` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`, `modified`)
VALUES (8, 1, 9, 1, 2, '2023-06-01 19:56:37'),
    (9, 3, 9, 3, 4, '2023-06-01 19:56:37'),
    (10, 8, 11, 5, 6, '2023-06-01 19:56:37'),
    (11, 8, 9, 7, 8, '2023-06-01 19:56:37'),
    (12, 1, 2, 9, 10, '2023-06-01 19:56:37'),
    (13, 1, 2, 11, 12, '2023-06-01 19:56:37');

# 8, 9 - GOOD;
# 10, 11 - BAD;
INSERT IGNORE INTO `{{targetSchema}}`.`gcd_issue_credit` (`id`, `creator_id`, `credit_type_id`, `issue_id`, `modified`)
VALUES (8, 8, 2, 9, '2023-06-01 19:56:37'),
    (9, 7, 3, 10, '2023-06-01 19:56:37'),
    (10, 6, 4, 11, '2023-06-01 19:56:37'),
    (11, 5, 5, 12, '2023-06-01 19:56:37');

UPDATE `{{targetSchema}}`.`gcd_publisher` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_series` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_indicia_publisher` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_issue` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_series_bond` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_story` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_story_credit` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_reprint` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_issue_credit` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_creator` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;
UPDATE `{{targetSchema}}`.`gcd_creator_name_detail` SET `modified` = '2023-06-01 19:56:37' WHERE `id` = 1;


