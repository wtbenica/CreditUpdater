# Adds issue_id and series_id to the story_credit and character_appearance tables.
#
# Updates the following tables:
# - gcd_story_credit
# - m_story_credit
# - m_character_appearance;

-- Add issue_id and series_id to gcd_story_credit;
UPDATE `{{targetSchema}}`.gcd_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM `{{targetSchema}}`.gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE `{{targetSchema}}`.gcd_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM `{{targetSchema}}`.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM `{{targetSchema}}`.gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE series_id IS NULL;

-- Add issue_id and series_id to m_story_credit;
UPDATE `{{targetSchema}}`.m_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM `{{targetSchema}}`.gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE `{{targetSchema}}`.m_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM `{{targetSchema}}`.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM `{{targetSchema}}`.gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE gsc.series_id IS NULL;

-- Create a view of stories with missing issue_id;
CREATE VIEW IF NOT EXISTS story_with_missing_issue AS
SELECT sy.id
FROM `{{targetSchema}}`.gcd_story sy
WHERE sy.issue_id NOT IN (
    SELECT gi.id
    FROM `{{targetSchema}}`.gcd_issue gi
);

-- Delete m_character_appearance records whose story is missing issue_id;
DELETE mca
FROM `{{targetSchema}}`.m_character_appearance mca
WHERE story_id IN (
    SELECT id
    FROM `{{targetSchema}}`.story_with_missing_issue);

-- Add issue_id and series_id to m_character_appearance;
UPDATE `{{targetSchema}}`.m_character_appearance mca
SET issue_id = (
    SELECT sy.issue_id
    FROM `{{targetSchema}}`.gcd_story sy
    WHERE sy.id = mca.story_id
    LIMIT 1)
WHERE issue_id IS NULL;

UPDATE `{{targetSchema}}`.m_character_appearance mca
SET series_id = (
    SELECT ie.series_id
    FROM `{{targetSchema}}`.gcd_issue ie
    WHERE ie.id = mca.issue_id
    LIMIT 1
)
WHERE series_id IS NULL;

-- Add NOT NULL constraints to issue_id and series_id columns in gcd_story_credit table;
ALTER TABLE `{{targetSchema}}`.gcd_story_credit
MODIFY COLUMN issue_id INT NOT NULL,
MODIFY COLUMN series_id INT NOT NULL;

-- add NOT NULL constraints to issue_id and series_id columns in m_character_appearance table;
ALTER TABLE `{{targetSchema}}`.m_character_appearance
MODIFY COLUMN issue_id INT NOT NULL,
MODIFY COLUMN series_id INT NOT NULL;

-- add NOT NULL constraints to issue_id and series_id columns in m_story_credit table;
ALTER TABLE `{{targetSchema}}`.m_story_credit
MODIFY COLUMN issue_id INT NOT NULL,
MODIFY COLUMN series_id INT NOT NULL;
