# If the issue and series columns don't already exist, then it adds them 
# to the gcd_story_credit and m_character_appearance tables
# Then creates the m_story_credit table;
###########################################################################
# Add issue/series columns to story_credit                                #
##########################################################################;
SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = 'new_gcd_dump'
  AND column_name = 'issue_id'
  AND table_name = 'gcd_story_credit';

SET @query_ngd_story_credit_issue_id_exists = IF(
    @exists <= 0,
    'ALTER TABLE new_gcd_dump.gcd_story_credit ADD COLUMN issue_id INT DEFAULT NULL',
    'SELECT \'Column exists\' status'
  );

PREPARE stmt
FROM @query_ngd_story_credit_issue_id_exists;

EXECUTE stmt;

SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = 'new_gcd_dump'
  AND table_name = 'gcd_story_credit'
  AND column_name = 'series_id';

SET @query_ngd_story_credit_series_id_exists = IF(
    @exists <= 0,
    'ALTER TABLE new_gcd_dump.gcd_story_credit ADD COLUMN series_id INT DEFAULT NULL',
    'SELECT \'Column exists\' status'
  );

PREPARE stmt
FROM @query_ngd_story_credit_series_id_exists;

EXECUTE stmt;

###########################################################################
# Create extracted item tables if not exist                               #
##########################################################################;
CREATE TABLE IF NOT EXISTS new_gcd_dump.m_character_appearance LIKE gcdb2.m_character_appearance;

CREATE TABLE IF NOT EXISTS new_gcd_dump.m_story_credit LIKE gcdb2.gcd_story_credit;

CREATE TABLE IF NOT EXISTS new_gcd_dump.m_character LIKE gcdb2.m_character;

# ###########################################################################
# Create views of 'good' items: publisher is US && > 1900                   #
# ##########################################################################;
DROP TABLE IF EXISTS new_gcd_dump.good_publishers;

CREATE TABLE new_gcd_dump.good_publishers AS
SELECT *
FROM new_gcd_dump.gcd_publisher
WHERE country_id = 225
  AND year_began >= 1900;

DROP TABLE IF EXISTS new_gcd_dump.publishers_to_migrate;

CREATE TABLE new_gcd_dump.publishers_to_migrate AS
SELECT new.*
FROM new_gcd_dump.good_publishers new
  LEFT JOIN gcdb2.gcd_publisher old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_publisher
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS new_gcd_dump.good_series;

CREATE TABLE new_gcd_dump.good_series AS
SELECT *
FROM new_gcd_dump.gcd_series
WHERE publisher_id IN (
    SELECT id
    FROM new_gcd_dump.good_publishers
  )
  AND country_id = 225
  AND language_id = 25;

DROP TABLE IF EXISTS new_gcd_dump.series_to_migrate;

CREATE TABLE new_gcd_dump.series_to_migrate AS
SELECT new.*
FROM new_gcd_dump.good_series new
  LEFT JOIN gcdb2.gcd_series old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_series
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS new_gcd_dump.good_issue;

CREATE TABLE new_gcd_dump.good_issue AS
SELECT *
FROM new_gcd_dump.gcd_issue
WHERE series_id IN (
    SELECT id
    FROM new_gcd_dump.good_series
  );

DROP TABLE IF EXISTS new_gcd_dump.issues_to_migrate;

CREATE TABLE new_gcd_dump.issues_to_migrate AS
SELECT new.*
FROM new_gcd_dump.good_issue new
  LEFT JOIN gcdb2.gcd_issue old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_issue
  )
  OR new.modified != old.modified;

DROP TABLE new_gcd_dump.good_story;

CREATE TABLE new_gcd_dump.good_story AS
SELECT *
FROM new_gcd_dump.gcd_story
WHERE issue_id IN (
    SELECT id
    FROM new_gcd_dump.good_issue
  )
  AND type_id IN (6, 19);

DROP TABLE IF EXISTS new_gcd_dump.stories_to_migrate;

CREATE TABLE new_gcd_dump.stories_to_migrate AS
SELECT new.*
FROM new_gcd_dump.good_story new
  LEFT JOIN gcdb2.gcd_story old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_story
  )
  OR new.modified != old.modified;

DROP TABLE IF EXISTS new_gcd_dump.good_story_credit;

CREATE TABLE new_gcd_dump.good_story_credit AS
SELECT *
FROM new_gcd_dump.gcd_story_credit
WHERE story_id IN (
    SELECT id
    FROM new_gcd_dump.good_story
  );

DROP TABLE IF EXISTS new_gcd_dump.story_credits_to_migrate;

CREATE TABLE new_gcd_dump.story_credits_to_migrate AS
SELECT new.*
FROM new_gcd_dump.good_story_credit new
  LEFT JOIN gcdb2.gcd_story_credit old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_story_credit
  )
  OR new.modified != old.modified;

# ###########################################################################
# # Add extracted item table constraints if not exist                       #
# ##########################################################################;
# SELECT COUNT(*)
# INTO @exist
# FROM information_schema.statistics
# WHERE table_schema = 'new_gcd_dump'
#   AND table_name = 'm_character'
#   AND column_name = 'name';
# 
# SET @query_add_index_m_character_name =
#     IF(@exist <= 0, 'CREATE INDEX m_character_name ON new_gcd_dump.m_character (name)',
#         'SELECT \'Index Exists\' status');
# 
# PREPARE stmt FROM @query_add_index_m_character_name;
# EXECUTE stmt;
# 
# SELECT COUNT(*)
# INTO @exist
# FROM information_schema.statistics
# WHERE table_schema = 'new_gcd_dump'
#   AND table_name = 'm_character'
#   AND column_name = 'alter_ego';
# 
# SET @query_add_index_m_character_alter_ego =
#         IF(@exist <= 0, 'CREATE INDEX m_character_alter_ego ON new_gcd_dump.m_character (alter_ego)',
#            'SELECT \'Index Exists\' status');
# 
# PREPARE stmt FROM @query_add_index_m_character_alter_ego;
# EXECUTE stmt;
# 
# SELECT COUNT(*)
# INTO @exist
# FROM information_schema.statistics
# WHERE table_schema = 'new_gcd_dump'
#   AND table_name = 'm_character_appearance'
#   AND index_name = 'detail_2';
# 
# SET @query_add_index_m_character_alter_ego =
#         IF(@exist <= 0, 'ALTER TABLE new_gcd_dump.m_character_appearance ADD INDEX (details, character_id, story_id, notes)',
#            'SELECT \'Index Exists\' status');
# 
# PREPARE stmt FROM @query_add_index_m_character_alter_ego;
# EXECUTE stmt;
# 
# 
# CREATE INDEX m_character_appearance_notes ON new_gcd_dump.m_character_appearance (notes);
# CREATE INDEX m_character_appearance_details ON new_gcd_dump.m_character_appearance (details);
# 
# ALTER TABLE new_gcd_dump.m_character_appearance
#     ADD FOREIGN KEY (story_id) REFERENCES gcd_story (id);
# ALTER TABLE new_gcd_dump.m_character_appearance
#     ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id);
# ALTER TABLE new_gcd_dump.m_character_appearance
#     ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id);