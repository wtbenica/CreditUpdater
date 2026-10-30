SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_character_role SELECT * FROM {{sourceSchema}}.gcd_character_role;
REPLACE {{targetSchema}}.gcd_character_relation_type SELECT * FROM {{sourceSchema}}.gcd_character_relation_type;
REPLACE {{targetSchema}}.gcd_group_relation_type SELECT * FROM {{sourceSchema}}.gcd_group_relation_type;
REPLACE {{targetSchema}}.gcd_character
SELECT new.*
FROM {{sourceSchema}}.gcd_character new
    LEFT JOIN {{targetSchema}}.gcd_character old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_character_name_detail
SELECT new.*
FROM {{sourceSchema}}.gcd_character_name_detail new
    LEFT JOIN {{targetSchema}}.gcd_character_name_detail old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_character_relation
SELECT new.*
FROM {{sourceSchema}}.gcd_character_relation new
    LEFT JOIN {{targetSchema}}.gcd_character_relation old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_group
SELECT new.*
FROM {{sourceSchema}}.gcd_group new
    LEFT JOIN {{targetSchema}}.gcd_group old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_group_name_detail
SELECT new.*
FROM {{sourceSchema}}.gcd_group_name_detail new
    LEFT JOIN {{targetSchema}}.gcd_group_name_detail old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_group_relation
SELECT new.*
FROM {{sourceSchema}}.gcd_group_relation new
    LEFT JOIN {{targetSchema}}.gcd_group_relation old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_story_character
SELECT new.*
FROM {{sourceSchema}}.gcd_story_character new
    JOIN {{targetSchema}}.gcd_story gs ON gs.id = new.story_id
    LEFT JOIN {{targetSchema}}.gcd_story_character old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_group_character
SELECT new.*
FROM {{sourceSchema}}.gcd_group_character new
    JOIN {{targetSchema}}.gcd_story gs ON gs.id = new.story_id
    LEFT JOIN {{targetSchema}}.gcd_group_character old ON new.id = old.id
WHERE old.id IS NULL OR new.modified != old.modified;
REPLACE {{targetSchema}}.gcd_story_character_group
SELECT sg.*
FROM {{sourceSchema}}.gcd_story_character_group sg
    JOIN {{targetSchema}}.gcd_story_character sc ON sc.id = sg.storycharacter_id;
DELETE sg
FROM {{targetSchema}}.gcd_story_character_group sg
    LEFT JOIN {{sourceSchema}}.gcd_story_character_group other ON other.id = sg.id
WHERE other.id IS NULL;
DELETE a
FROM {{targetSchema}}.m_character_appearance a
    LEFT JOIN {{targetSchema}}.gcd_story_character sc ON sc.id = a.gcd_story_character_id
WHERE a.gcd_story_character_id IS NOT NULL AND (sc.id IS NULL OR sc.deleted = 1);
DELETE a
FROM {{targetSchema}}.m_character_appearance a
    LEFT JOIN {{targetSchema}}.gcd_group_character gc ON gc.id = a.gcd_group_character_id
WHERE a.gcd_group_character_id IS NOT NULL AND (gc.id IS NULL OR gc.deleted = 1);
SET FOREIGN_KEY_CHECKS = 1;
