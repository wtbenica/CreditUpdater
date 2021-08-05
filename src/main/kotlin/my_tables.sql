# If the issue and series columns don't already exist, then it adds them 
# to the gcd_story_credit and m_character_appearance tables
# Then creates the m_story_credit table

SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = 'gcdb2'
  AND column_name = 'issue'
  AND table_name = 'gcd_story_credit'
LIMIT 1;

SET @query_add_issue_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit ADD COLUMN issue INT DEFAULT NULL',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_issue_to_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'gcd_story_credit'
  AND column_name = 'issue'
  AND referenced_table_name = 'gcd_issue'
  AND referenced_column_name = 'id';

SET @query_add_fk_issue_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit ADD FOREIGN KEY (issue) REFERENCES gcd_issue (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_issue_to_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = 'gcdb2'
  AND column_name = 'series'
  AND table_name = 'gcd_story_credit'
LIMIT 1;

SET @query_add_series_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit ADD COLUMN series INT DEFAULT NULL',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_series_to_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'gcd_story_credit'
  AND column_name = 'series'
  AND referenced_table_name = 'gcd_series'
  AND referenced_column_name = 'id';

SET @query_add_fk_series_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.gcd_story_credit ADD FOREIGN KEY (series) REFERENCES gcd_series (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_series_to_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = 'gcdb2'
  AND column_name = 'issue'
  AND table_name = 'm_character_appearance'
LIMIT 1;

SET @query_add_issue_to_character_appearance =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.m_character_appearance ADD COLUMN issue INT DEFAULT NULL',
           'SELECT \'Column Exists\' status');

PREPARE stmt FROM @query_add_issue_to_character_appearance;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_character_appearance'
  AND column_name = 'issue'
  AND referenced_table_name = 'gcd_issue'
  AND referenced_column_name = 'id';

SET @query_add_fk_issue_to_character_appearance =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.m_character_appearance ADD FOREIGN KEY (issue) REFERENCES gcd_issue (id)',
           'SELECT \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_issue_to_character_appearance;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = 'gcdb2'
  AND column_name = 'series'
  AND table_name = 'm_character_appearance'
LIMIT 1;

SET @query_add_series_to_character_appearance =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.m_character_appearance ADD COLUMN series INT DEFAULT NULL',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_series_to_character_appearance;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_character_appearance'
  AND column_name = 'series'
  AND referenced_table_name = 'gcd_series'
  AND referenced_column_name = 'id';

SET @query_add_fk_series_to_character_appearance =
        IF(@exist <= 0, 'ALTER TABLE gcdb2.m_character_appearance ADD FOREIGN KEY (series) REFERENCES gcd_series (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_series_to_character_appearance;
EXECUTE stmt;


CREATE TABLE IF NOT EXISTS m_story_credit AS (
    SELECT *
    FROM gcd_story_credit
    WHERE FALSE
);

SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_story_credit'
  AND constraint_name = 'PRIMARY';

SET @query_add_pk_id_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE m_story_credit MODIFY COLUMN id INT PRIMARY KEY AUTO_INCREMENT',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_creator_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_story_credit'
  AND column_name = 'creator_id'
  AND referenced_table_name = 'gcd_creator_name_detail'
  AND referenced_column_name = 'id';

SET @query_add_fk_creator_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE m_story_credit ADD FOREIGN KEY (creator_id) REFERENCES gcd_creator_name_detail(id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_creator_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_story_credit'
  AND column_name = 'credit_type_id'
  AND referenced_table_name = 'gcd_credit_type'
  AND referenced_column_name = 'id';

SET @query_add_fk_credit_type_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE m_story_credit ADD FOREIGN KEY (credit_type_id) REFERENCES gcd_credit_type (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_credit_type_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_story_credit'
  AND column_name = 'issue'
  AND referenced_table_name = 'gcd_issue'
  AND referenced_column_name = 'id';

SET @query_add_fk_issue_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE m_story_credit ADD FOREIGN KEY (issue) REFERENCES gcd_issue (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_issue_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_story_credit'
  AND column_name = 'series'
  AND referenced_table_name = 'gcd_series'
  AND referenced_column_name = 'id';

SET @query_add_fk_series_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE m_story_credit ADD FOREIGN KEY (series) REFERENCES gcd_series (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_series_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = 'gcdb2'
  AND table_name = 'm_story_credit'
  AND column_name = 'story_id'
  AND referenced_table_name = 'gcd_story'
  AND referenced_column_name = 'id';

SET @query_add_fk_story_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE m_story_credit ADD FOREIGN KEY (story_id) REFERENCES gcd_story (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_story_to_m_story_credit;
EXECUTE stmt;