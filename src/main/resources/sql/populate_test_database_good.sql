INSERT IGNORE INTO `gcd_publisher` (`id`, `name`, `country_id`, `year_began`, `modified`)
VALUES (1, 'Marvel', 225, 1939, '2004-06-01 19:56:37'),
    (2, 'DC', 225, 1935, '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_series` (
        `id`,
        `name`,
        `year_began`,
        `publisher_id`,
        `country_id`,
        `language_id`,
        `modified`
    )
VALUES (1, 'Doom Patrol', 1988, 2, 225, 25, '2004-06-01 19:56:37'),
    (2, 'New X-Men', 2001, 1, 225, 25, '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_issue` (`id`, `number`, `series_id`, `modified`)
VALUES (1, 35, 1, '2004-06-01 19:56:37'),
    (2, 114, 2, '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_series_bond` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`, `modified`)
VALUES (1, 1, 2, 1, 2, '2004-06-01 19:56:37'),
    (2, 2, 1, 2, 1, '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_story` (
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
        1,
        'Crawling from the Wreckage',
        1,
        'Grant Morrison',
        'Richard Case',
        'Richard Case',
        'Daniel Vozzo',
        'John Workman',
        'Tom Peyer',
        'Doom Patrol [Crazy Jane [Kay Challis]; Robotman [Cliff Steele]; Dorothy Spinner; Rebis [Larry Trainor]]; Danny the Street; Flex Mentallo (cameo, unnamed); Willoughby Kipling;',
        '2004-06-01 19:56:37'
    ),
    (
        2,
        'E for Extinction',
        2,
        'Grant Morrison',
        'Frank Quitely',
        'Tim Townsend',
        'Liquid!',
        'Richard Starkings',
        'Mark Powers',
        'X-Men [Beast [Hank McCoy]; Cyclops [Scott Summers]; White Queen [Emma Frost]; Marvel Girl [Jean Grey]; Professor X [Charles Xavier]; Wolverine [Logan]];',
        '2004-06-01 19:56:37'
    );

INSERT IGNORE INTO `gcd_credit_type` (`id`, `name`, `modified`)
VALUES (1, 'script', '2004-06-01 19:56:37'),
    (2, 'pencils', '2004-06-01 19:56:37'),
    (3, 'inks', '2004-06-01 19:56:37'),
    (4, 'colors', '2004-06-01 19:56:37'),
    (5, 'letters', '2004-06-01 19:56:37'),
    (6, 'editing', '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_creator_name_detail` (`id`, `name`, `modified`)
VALUES (1, 'Grant Morrison', '2004-06-01 19:56:37'),
    (2, 'Frank Quitely', '2004-06-01 19:56:37'),
    (3, 'Val Semeiks', '2004-06-01 19:56:37'),
    (4, 'Dan Green', '2004-06-01 19:56:37'),
    (5, 'Chris Sotomayor', '2004-06-01 19:56:37'),
    (6, 'Richard Starkings', '2004-06-01 19:56:37'),
    (7, 'Bob Schreck', '2004-06-01 19:56:37'),
    (8, 'Michael Wright', '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_story_credit` (`id`, `creator_id`, `credit_type_id`, `story_id`, `modified`)
VALUES (1, 2, 2, 1, '2004-06-01 19:56:37'),
    (2, 3, 2, 2, '2004-06-01 19:56:37'),
    (3, 4, 3, 2, '2004-06-01 19:56:37'),
    (4, 5, 4, 2, '2004-06-01 19:56:37'),
    (5, 6, 5, 2, '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_reprint` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`, `modified`)
VALUES (1, 1, 2, 1, 2, '2004-06-01 19:56:37'),
    (2, 2, 1, 2, 1, '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_issue_credit` (`id`, `creator_id`, `credit_type_id`, `issue_id`, `modified`)
VALUES (1, 1, 1, 1, '2004-06-01 19:56:37'),
    (2, 2, 2, 1, '2004-06-01 19:56:37');

INSERT IGNORE INTO `gcd_indicia_publisher` (`id`, `name`, `parent_id`, `modified`)
VALUES (1, 'Marvel Comics', 1, '2004-06-01 19:56:37'),
    (2, 'DC Comics', 2, '2004-06-01 19:56:37');