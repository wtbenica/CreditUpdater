INSERT IGNORE INTO m_character (
        `id`,
        `name`,
        `alter_ego`,
        `publisher_id`
    )
VALUES (1, 'Batman', 'Bruce Wayne', 2),
    (2, 'Batgirl', 'Barbara Gordon', 2);

INSERT IGNORE INTO m_character_appearance (
        `id`,
        `details`,
        `character_id`,
        `story_id`,
        `notes`,
        `membership`
    )
VALUES (1, 'cameo', 1, 1, NULL, NULL),
    (2, 'first appearance', 2, 1, NULL, NULL);

