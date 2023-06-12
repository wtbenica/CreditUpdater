# If the issue and series columns don't already exist, then it adds them 
# to the gcd_story_credit and m_character_appearance tables
# Then creates the m_story_credit table;

###########################################################################
# Add issue/series columns to story_credit and fk constraints             #
##########################################################################;
SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = '<schema>'
  AND column_name = 'issue_id'
  AND table_name = 'gcd_story_credit'
LIMIT 1;

SET @query_add_issue_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE <schema>.gcd_story_credit ADD COLUMN issue_id INT DEFAULT NULL',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_issue_to_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'gcd_story_credit'
  AND column_name = 'issue_id'
  AND referenced_table_name = 'gcd_issue'
  AND referenced_column_name = 'id';

SET @query_add_fk_issue_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE <schema>.gcd_story_credit ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_issue_to_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.columns
WHERE table_schema = '<schema>'
  AND column_name = 'series_id'
  AND table_name = 'gcd_story_credit'
LIMIT 1;

SET @query_add_series_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE <schema>.gcd_story_credit ADD COLUMN series_id INT DEFAULT NULL',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_series_to_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'gcd_story_credit'
  AND column_name = 'series_id'
  AND referenced_table_name = 'gcd_series'
  AND referenced_column_name = 'id';

SET @query_add_fk_series_to_story_credit =
        IF(@exist <= 0, 'ALTER TABLE <schema>.gcd_story_credit ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_series_to_story_credit;
EXECUTE stmt;

###########################################################################
# Create extracted item tables if not exist                               #
##########################################################################;
CREATE TABLE IF NOT EXISTS m_character
(
    id           INTEGER PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(255) NOT NULL,
    alter_ego    VARCHAR(255),
    publisher_id INTEGER REFERENCES gcd_publisher (id),
    INDEX (name),
    INDEX (alter_ego),
    UNIQUE INDEX (name, alter_ego, publisher_id)
);

CREATE TABLE IF NOT EXISTS m_character_appearance
(
    id           INTEGER PRIMARY KEY AUTO_INCREMENT,
    details      VARCHAR(255),
    character_id    INTEGER NOT NULL,
    story_id     INTEGER NOT NULL,
    notes        VARCHAR(255),
    membership   LONGTEXT,
    issue_id     INTEGER DEFAULT NULL,
    series_id    INTEGER REFERENCES new_gcd_dump.gcd_series (id),
    FOREIGN KEY (character_id) REFERENCES m_character (id),
    FOREIGN KEY (story_id) REFERENCES gcd_story (id),
    FOREIGN KEY (issue_id) REFERENCES gcd_issue (id),
    FOREIGN KEY (series_id) REFERENCES gcd_series (id),
    INDEX (notes),
    INDEX (details),
    UNIQUE INDEX (details, character_id, story_id, notes)
);

CREATE TABLE IF NOT EXISTS m_story_credit LIKE gcd_story_credit;

###########################################################################
# Add constraints to m_story_credit                                       #
##########################################################################;
SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'm_story_credit'
  AND constraint_name = 'PRIMARY';

SET @query_add_pk_id_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE <schema>.m_story_credit MODIFY COLUMN id INT PRIMARY KEY AUTO_INCREMENT',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_creator_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'm_story_credit'
  AND column_name = 'creator_id'
  AND referenced_table_name = 'gcd_creator_name_detail'
  AND referenced_column_name = 'id';

SET @query_add_fk_creator_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE <schema>.m_story_credit ADD FOREIGN KEY (creator_id) REFERENCES gcd_creator_name_detail(id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_creator_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'm_story_credit'
  AND column_name = 'credit_type_id'
  AND referenced_table_name = 'gcd_credit_type'
  AND referenced_column_name = 'id';

SET @query_add_fk_credit_type_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE <schema>.m_story_credit ADD FOREIGN KEY (credit_type_id) REFERENCES gcd_credit_type (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_credit_type_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'm_story_credit'
  AND column_name = 'issue_id'
  AND referenced_table_name = 'gcd_issue'
  AND referenced_column_name = 'id';

SET @query_add_fk_issue_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE <schema>.m_story_credit ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_issue_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'm_story_credit'
  AND column_name = 'series_id'
  AND referenced_table_name = 'gcd_series'
  AND referenced_column_name = 'id';

SET @query_add_fk_series_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE <schema>.m_story_credit ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_series_to_m_story_credit;
EXECUTE stmt;


SELECT COUNT(*)
INTO @exist
FROM information_schema.key_column_usage
WHERE table_schema = '<schema>'
  AND table_name = 'm_story_credit'
  AND column_name = 'story_id'
  AND referenced_table_name = 'gcd_story'
  AND referenced_column_name = 'id';

SET @query_add_fk_story_to_m_story_credit =
        IF(@exist <= 0,
           'ALTER TABLE <schema>.m_story_credit ADD FOREIGN KEY (story_id) REFERENCES gcd_story (id)',
           'select \'Column Exists\' status');

PREPARE stmt FROM @query_add_fk_story_to_m_story_credit;
EXECUTE stmt;