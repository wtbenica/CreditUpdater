SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = 'gcdb2'
  AND column_name = 'issue'
  AND table_name = 'gcd_story_credit'
LIMIT 1;

SET @query = IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit  ADD COLUMN issue INT DEFAULT NULL',
                'select \'Column Exists\' status');
SET @query2 = IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit  ADD FOREIGN KEY (issue) REFERENCES gcd_series (id)',
                 'select \'Column Exists\' status');

SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = 'gcdb2'
  AND column_name = 'series'
  AND table_name = 'gcd_story_credit'
LIMIT 1;

SET @query3 = IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit  ADD COLUMN series INT DEFAULT NULL',
                 'select \'Column Exists\' status');
SET @query4 = IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit  ADD FOREIGN KEY (series) REFERENCES gcd_series (id)',
                 'select \'Column Exists\' status');

PREPARE stmt FROM @query;
EXECUTE stmt;
PREPARE stmt FROM @query2;
EXECUTE stmt;
PREPARE stmt FROM @query3;
EXECUTE stmt;
PREPARE stmt FROM @query4;
EXECUTE stmt;

CREATE TABLE IF NOT EXISTS m_story_credit AS (
    SELECT *
    FROM gcd_story_credit
    WHERE FALSE
)