UPDATE {{sourceSchema}}.gcd_story_credit credit
SET credit.issue_id = (
    SELECT story.issue_id
    FROM {{sourceSchema}}.gcd_story story
    WHERE story.id = credit.story_id
    LIMIT 1
)
WHERE issue_id IS NULL
  AND credit.story_id IN (
    SELECT id
    FROM {{sourceSchema}}.migrate_stories
);

UPDATE {{sourceSchema}}.gcd_story_credit credit
SET credit.series_id = (
    SELECT issue.series_id
    FROM {{sourceSchema}}.gcd_issue issue
    WHERE issue.id IN (
        SELECT story.issue_id
        FROM {{sourceSchema}}.gcd_story story
        WHERE story.id = credit.story_id
    )
    LIMIT 1
)
WHERE series_id IS NULL
  AND story_id IN (
    SELECT id
    FROM {{sourceSchema}}.migrate_stories
);

UPDATE {{sourceSchema}}.m_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM {{sourceSchema}}.gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE {{sourceSchema}}.m_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM {{sourceSchema}}.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM {{sourceSchema}}.gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE gsc.series_id IS NULL;

UPDATE {{sourceSchema}}.m_character_appearance mca
SET issue_id = (
    SELECT sy.issue_id
    FROM {{sourceSchema}}.gcd_story sy
    WHERE sy.id = mca.story_id
    LIMIT 1)
WHERE issue_id IS NULL;

UPDATE {{sourceSchema}}.m_character_appearance mca
SET series_id = (
    SELECT ie.series_id
    FROM {{sourceSchema}}.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM {{sourceSchema}}.gcd_story sy
        WHERE sy.id = mca.story_id
    )
)
WHERE series_id IS NULL;
