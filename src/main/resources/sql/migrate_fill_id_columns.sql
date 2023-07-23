UPDATE {{targetSchema}}.migrate_story_credits msc
SET msc.issue_id = (
    SELECT issue_id
    FROM {{targetSchema}}.gcd_story
    WHERE id = msc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE {{targetSchema}}.migrate_story_credits msc
SET msc.series_id = (
    SELECT series_id
    FROM {{targetSchema}}.gcd_issue
    WHERE id = (
        SELECT issue_id
        FROM {{targetSchema}}.gcd_story
        WHERE id = msc.story_id
        LIMIT 1
    )
    LIMIT 1
)
WHERE series_id IS NULL;

UPDATE {{targetSchema}}.gcd_story_credit credit
SET credit.issue_id = (
    SELECT story.issue_id
    FROM {{targetSchema}}.gcd_story story
    WHERE story.id = credit.story_id
    LIMIT 1
)
WHERE issue_id IS NULL
  AND credit.story_id IN (
    SELECT id
    FROM {{targetSchema}}.migrate_stories
);

UPDATE {{targetSchema}}.gcd_story_credit credit
SET credit.series_id = (
    SELECT issue.series_id
    FROM {{targetSchema}}.gcd_issue issue
    WHERE issue.id IN (
        SELECT story.issue_id
        FROM {{targetSchema}}.gcd_story story
        WHERE story.id = credit.story_id
    )
    LIMIT 1
)
WHERE series_id IS NULL
  AND story_id IN (
    SELECT id
    FROM {{targetSchema}}.migrate_stories
);

UPDATE {{targetSchema}}.m_story_credit gsc
SET issue_id = (
    SELECT sy.issue_id
    FROM {{targetSchema}}.gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1
)
WHERE issue_id IS NULL;

UPDATE {{targetSchema}}.m_story_credit gsc
SET series_id = (
    SELECT ie.series_id
    FROM {{targetSchema}}.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM {{targetSchema}}.gcd_story sy
        WHERE sy.id = gsc.story_id
    )
    LIMIT 1
)
WHERE gsc.series_id IS NULL;

UPDATE {{targetSchema}}.m_character_appearance mca
SET issue_id = (
    SELECT sy.issue_id
    FROM {{targetSchema}}.gcd_story sy
    WHERE sy.id = mca.story_id
    LIMIT 1)
WHERE issue_id IS NULL;

UPDATE {{targetSchema}}.m_character_appearance mca
SET series_id = (
    SELECT ie.series_id
    FROM {{targetSchema}}.gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM {{targetSchema}}.gcd_story sy
        WHERE sy.id = mca.story_id
    )
)
WHERE series_id IS NULL;
