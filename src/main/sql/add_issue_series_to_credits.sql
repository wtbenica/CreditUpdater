UPDATE gcd_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE gcd_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE series_id IS NULL;

UPDATE m_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE m_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE gsc.series_id IS NULL;

#
CREATE TEMPORARY TABLE story_with_missing_issue AS
SELECT sy.id
FROM gcdb2.gcd_story sy
WHERE sy.issue_id NOT IN (
    SELECT gi.id
    FROM gcdb2.gcd_issue gi
);

DELETE
FROM gcdb2.m_character_appearance mca
WHERE story_id IN (
    SELECT id
    FROM story_with_missing_issue);

UPDATE m_character_appearance mca
SET issue_id = (
    SELECT sy.issue_id
    FROM gcd_story sy
    WHERE sy.id = mca.story_id
    LIMIT 1)
WHERE issue_id IS NULL;

UPDATE m_character_appearance mca
SET series_id = (
    SELECT ie.series_id
    FROM gcd_issue ie
    WHERE ie.id = mca.issue_id
    LIMIT 1
)
WHERE series_id IS NULL;