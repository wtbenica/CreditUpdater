INSERT IGNORE INTO m_character (
        `id`,
        `name`,
        `alter_ego`,
        `publisher_id`
    )
VALUES (1, 'Doom Patrol', NULL, 2),
    (2, 'Danny the Street', NULL, 2),
    (3, 'Flex Mentallo', NULL, 2),
    (4, 'Willoughby Kipling', NULL, 2),
    (5, 'X-Men', NULL, 1);

INSERT IGNORE INTO m_character_appearance (
        `id`,
        `details`,
        `character_id`,
        `story_id`,
        `notes`,
        `membership`
    )
VALUES (1, NULL, 1, 1, NULL, 'Crazy Jane [Kay Challis]; Robotman [Cliff Steele]; Dorothy Spinner; Rebis [Larry Trainor]'),
    (2, NULL, 2, 1, NULL, NULL),
    (3, 'cameo, unnamed', 3, 1, NULL, NULL),
    (4, NULL, 4, 1, NULL, NULL),
    (5, NULL, 5, 2, NULL, 'Beast [Hank McCoy]; Cyclops [Scott Summers]; White Queen [Emma Frost]; Marvel Girl [Jean Grey]; Professor X [Charles Xavier]; Wolverine [Logan]');
