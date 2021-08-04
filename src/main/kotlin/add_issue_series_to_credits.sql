UPDATE gcd_story_credit gsc
SET issue = (
    SELECT sy.issue_id
    FROM gcd_story sy
    WHERE sy.id = gsc.story_id
    LIMIT 1)
WHERE issue IS NULL;

UPDATE gcd_story_credit gsc
SET series = (
    SELECT ie.series_id
    FROM gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM gcd_story sy
        WHERE sy.id = gsc.story_id
    )
)
WHERE series IS NULL;

UPDATE m_story_credit gsc
SET issue = (
    SELECT sy.issue_id
    FROM gcd_story sy
    WHERE sy.id = gsc.story_id
)
WHERE issue IS NULL;

UPDATE m_story_credit gsc
SET series = (
    SELECT ie.series_id
    FROM gcd_issue ie
    WHERE ie.id IN (
        SELECT sy.issue_id
        FROM gcd_story sy
        WHERE sy.id = gsc.story_id
    )
)
WHERE series IS NULL;