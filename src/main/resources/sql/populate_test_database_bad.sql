INSERT IGNORE INTO `gcd_publisher` (`id`, `name`, `country_id`, `year_began`)
VALUES (3, 'BAD country_id != 225', 106, 1901),
    (4, 'BAD no series >= 1900', 225, 1817),
    (5, 'GOOD old pub, series >= 1900', 225, 1800);

INSERT IGNORE INTO `gcd_series` (
        `id`,
        `name`,
        `year_began`,
        `publisher_id`,
        `country_id`,
        `language_id`,
        `first_issue_id`,
        `last_issue_id`
    )
VALUES (3, 'BAD bad pub', 1990, 3, 106, 51, 1, 2),
    (4, 'BAD < 1900, bad pub', 1899, 4, 225, 25, 2, 1),
    (5, 'BAD language_id != 25', 2010, 2, 225, 34, 1, 2),
    (6, 'BAD country_id != 225', 1983, 1, 36, 25, 2, 1),
    (7, 'BAD < 1900, good pub', 1890, 2, 225, 25, 1, 1),
    (8, 'GOOD >= 1900, old pub', 1900, 5, 225, 25, 2, 2),
    (9, 'BAD first_issue_id', 2000, 2, 225, 25, 7, 2),
    (10, 'BAD last_issue_id', 2000, 2, 225, 25, 1, 7);

INSERT IGNORE INTO `gcd_issue` (`id`, `number`, `series_id`)
VALUES (3, 1, 3),
    (4, 1, 4),
    (5, 1, 5),
    (6, 1, 6),
    (7, 1, 7);

INSERT IGNORE INTO `gcd_series_bond` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`)
VALUES (3, 1, 3, 1, 2),
    (4, 4, 2, 1, 2),
    (5, 4, 5, 1, 2),
    (6, 1, 2, 3, 4),
    (7, 2, 1, 5, 6);

INSERT IGNORE INTO `gcd_story` (`id`, `title`, `issue_id`)
VALUES (3, 'BAD issue 3', 3),
    (4, 'BAD issue 4', 4),
    (5, 'BAD issue 5', 5),
    (6, 'BAD issue 6', 6),
    (7, 'BAD issue 7', 7);

INSERT IGNORE INTO `gcd_story_credit` (`id`, `creator_id`, `credit_type_id`, `story_id`)
VALUES (6, 1, 2, 3),
    (7, 1, 3, 4),
    (8, 1, 4, 5),
    (9, 1, 5, 6),
    (10, 1, 6, 7);

INSERT IGNORE INTO `gcd_reprint` (`id`, `origin_id`, `target_id`, `origin_issue_id`, `target_issue_id`)
VALUES (3, 1, 3, 1, 2),
    (4, 4, 2, 1, 2),
    (5, 4, 5, 1, 2),
    (6, 1, 2, 3, 4),
    (7, 2, 1, 5, 6);