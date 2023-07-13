INSERT IGNORE INTO `gcd_publisher` (`id`, `name`, `country_id`, `year_began`)
VALUES (1, 'Marvel', 225, 1939),
    (2, 'DC', 225, 1935);

INSERT IGNORE INTO `gcd_series` (
        `id`,
        `name`,
        `year_began`,
        `publisher_id`,
        `country_id`,
        `language_id`
    )
VALUES (1, 'Doom Patrol', 1988, 2, 225, 25),
    (2, 'New X-Men', 2001, 1, 225, 25);

INSERT IGNORE INTO `gcd_issue` (`id`, `number`, `series_id`)
VALUES (1, 35, 1),
    (2, 114, 2);

INSERT IGNORE INTO `gcd_series_bond` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`)
VALUES (1, 1, 2, 1, 2),
    (2, 2, 1, 2, 1);

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
        `characters`
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
        'Doom Patrol [Crazy Jane [Kay Challis], Robotman [Cliff Steele], Dorothy Spinner, Rebis [Larry Trainor]], Danny the Street, Flex Mentallo, Willoughby Kipling'
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
        'X-Men [Beast [Hank McKoy], Cyclops [Scott Summers], White Queen [Emma Frost], Marvel Girl [Jean Grey], Professor X [Charles Xavier], Wolverine [Logan]]'
    );

INSERT IGNORE INTO `gcd_credit_type` (`id`, `name`)
VALUES (1, 'script'),
    (2, 'pencils'),
    (3, 'inks'),
    (4, 'colors'),
    (5, 'letters'),
    (6, 'editing');

INSERT IGNORE INTO `gcd_creator_name_detail` (`id`, `name`)
VALUES (1, 'Grant Morrison'),
    (2, 'Frank Quitely'),
    (3, 'Val Semeiks'),
    (4, 'Dan Green'),
    (5, 'Chris Sotomayor'),
    (6, 'Richard Starkings'),
    (7, 'Bob Schreck'),
    (8, 'Michael Wright');

INSERT IGNORE INTO `gcd_story_credit` (`id`, `creator_id`, `credit_type_id`, `story_id`)
VALUES (1, 2, 2, 1),
    (2, 3, 2, 2),
    (3, 4, 3, 2),
    (4, 5, 4, 2),
    (5, 6, 5, 2);

INSERT IGNORE INTO `gcd_reprint` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`)
VALUES (1, 1, 2, 1, 2),
    (2, 2, 1, 2, 1);

INSERT IGNORE INTO `gcd_issue_credit` (`id`, `creator_id`, `credit_type_id`, `issue_id`)
VALUES (1, 1, 1, 1),
    (2, 2, 2, 1);

INSERT IGNORE INTO `gcd_indicia_publisher` (`id`, `name`, `parent_id`)
VALUES (1, 'Marvel Comics', 1),
    (2, 'DC Comics', 2);