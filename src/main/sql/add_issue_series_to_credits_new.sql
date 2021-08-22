CREATE VIEW new_gcd_dump.good_publishers AS
SELECT *
FROM new_gcd_dump.gcd_publisher
WHERE country_id = 225
  AND year_began >= 1900;

CREATE VIEW new_gcd_dump.good_series AS
SELECT *
FROM new_gcd_dump.gcd_series
WHERE publisher_id IN (
    SELECT id
    FROM new_gcd_dump.good_publishers
);

CREATE VIEW new_gcd_dump.good_issue AS
SELECT *
FROM new_gcd_dump.gcd_issue
WHERE series_id IN (
    SELECT id
    FROM new_gcd_dump.good_series
);

CREATE VIEW new_gcd_dump.good_story AS
SELECT *
FROM new_gcd_dump.gcd_story
WHERE issue_id IN (
    SELECT id
    FROM new_gcd_dump.good_issue
)
  AND type_id IN (6, 19);

CREATE VIEW new_gcd_dump.good_story_credit AS
SELECT *
FROM new_gcd_dump.gcd_story_credit
WHERE story_id IN (
    SELECT id
    FROM new_gcd_dump.good_story
);

UPDATE new_gcd_dump.gcd_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM new_gcd_dump.gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL
  AND gsc.story_id IN (
    SELECT id
    FROM new_gcd_dump.good_story
);

UPDATE new_gcd_dump.gcd_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM new_gcd_dump.gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE series_id IS NULL
  AND story_id IN (
    SELECT id
    FROM new_gcd_dump.good_story
);

UPDATE new_gcd_dump.m_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM new_gcd_dump.gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE new_gcd_dump.m_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM new_gcd_dump.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM new_gcd_dump.gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE gsc.series_id IS NULL;

UPDATE new_gcd_dump.m_character_appearance mca
SET issue_id = (
    SELECT sy.issue_id
    FROM new_gcd_dump.gcd_story sy
    WHERE sy.id = mca.story_id
    LIMIT 1)
WHERE issue_id IS NULL;

UPDATE new_gcd_dump.m_character_appearance mca
SET series_id = (
    SELECT ie.series_id
    FROM new_gcd_dump.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM new_gcd_dump.gcd_story sy
        WHERE sy.id = mca.story_id
    )
)
WHERE series_id IS NULL;
