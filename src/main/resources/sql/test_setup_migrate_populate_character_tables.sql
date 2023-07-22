INSERT IGNORE INTO {{targetSchema}}.m_character (
        `id`,
        `name`,
        `alter_ego`,
        `publisher_id`
    )
VALUES (1, 'Doom Patrol', NULL, 2),
    (2, 'Danny the Street', NULL, 2),
    (3, 'Flex Mentallo', NULL, 2),
    (4, 'Willoughby Kipling', NULL, 2),
    (5, 'Individual', 'Alter Ego', 2),
    (6, 'Team', NULL, 2);

INSERT IGNORE INTO {{targetSchema}}.m_character_appearance (
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
    (5, 'detail', 6, 8, NULL, NULL),
    (6, NULL, 7, 8, NULL, 'Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)'),
    (7, 'detail', 6, 9, NULL, NULL),
    (8, NULL, 7, 9, NULL, 'Member 1 [AE 1] (detail 1); Member 2 [AE 2] (detail 2)')
