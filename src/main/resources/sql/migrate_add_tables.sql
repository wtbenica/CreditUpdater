# If the issue and series columns don't already exist, then it adds them 
# to the gcd_story_credit and m_character_appearance tables
# Then creates the m_story_credit table;
###########################################################################
# Add issue/series columns to story_credit                                #
##########################################################################;
SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = '{{sourceSchema}}'
  AND column_name = 'issue_id'
  AND table_name = 'gcd_story_credit';

SET @query_ngd_story_credit_issue_id_exists = IF(
    @exists <= 0,
    'ALTER TABLE {{sourceSchema}}.gcd_story_credit ADD COLUMN issue_id INT DEFAULT NULL',
    'SELECT \'Column exists\' status'
  );

PREPARE stmt
FROM @query_ngd_story_credit_issue_id_exists;

EXECUTE stmt;

SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = '{{sourceSchema}}'
  AND table_name = 'gcd_story_credit'
  AND column_name = 'series_id';

SET @query_ngd_story_credit_series_id_exists = IF(
    @exists <= 0,
    'ALTER TABLE {{sourceSchema}}.gcd_story_credit ADD COLUMN series_id INT DEFAULT NULL',
    'SELECT \'Column exists\' status'
  );

PREPARE stmt
FROM @query_ngd_story_credit_series_id_exists;

EXECUTE stmt;

###########################################################################
# Create extracted item tables if not exist                               #
##########################################################################;
CREATE TABLE IF NOT EXISTS {{sourceSchema}}.m_character_appearance LIKE {{targetSchema}}.m_character_appearance;

CREATE TABLE IF NOT EXISTS {{sourceSchema}}.m_story_credit LIKE {{targetSchema}}.gcd_story_credit;

CREATE TABLE IF NOT EXISTS {{sourceSchema}}.m_character LIKE {{targetSchema}}.m_character;

# ###########################################################################
# Create views of 'good' publishers: publisher is US && has series >= 1900       #
# ##########################################################################;
DROP TABLE IF EXISTS {{sourceSchema}}.good_publishers;

CREATE TABLE {{sourceSchema}}.good_publishers AS
SELECT DISTINCT gp.*
FROM {{sourceSchema}}.gcd_publisher gp
JOIN {{sourceSchema}}.gcd_series gs ON gp.id = gs.publisher_id
WHERE gp.country_id = 225
  AND  gs.year_began >= 1900;

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_publishers;

CREATE TABLE {{sourceSchema}}.migrate_publishers AS
SELECT new.*
FROM {{sourceSchema}}.good_publishers new
  LEFT JOIN {{targetSchema}}.gcd_publisher old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_publisher
  )
  OR new.modified != old.modified;

# ##############################################################################
# Create views of 'good' indicia publishers: indicia publisher parent_id is good_publishers #
# #############################################################################;

DROP TABLE IF EXISTS {{sourceSchema}}.good_indicia_publishers;

CREATE TABLE {{sourceSchema}}.good_indicia_publishers AS
SELECT gip.*
FROM {{sourceSchema}}.gcd_indicia_publisher gip
WHERE gip.parent_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_publishers
  );

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_indicia_publishers;

CREATE TABLE {{sourceSchema}}.migrate_indicia_publishers AS
SELECT new.*
FROM {{sourceSchema}}.good_indicia_publishers new
  LEFT JOIN {{targetSchema}}.gcd_indicia_publisher old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_indicia_publisher
  )
  OR new.modified != old.modified;

# ##############################################################################
# Create views of 'good' series: series publisher_id is good_publishers and     #
# series year_began >= 1900, language_id = 25, and country_id = 225            #
# #############################################################################;

DROP TABLE IF EXISTS {{sourceSchema}}.good_series;

CREATE TABLE {{sourceSchema}}.good_series AS
SELECT *
FROM {{sourceSchema}}.gcd_series
WHERE publisher_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_publishers
  )
  AND country_id = 225
  AND language_id = 25
  AND year_began >= 1900;

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_series;

CREATE TABLE {{sourceSchema}}.migrate_series AS
SELECT new.*
FROM {{sourceSchema}}.good_series new
  LEFT JOIN {{targetSchema}}.gcd_series old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_series
  )
  OR new.modified != old.modified;

# ##############################################################################
# Create views of 'good' issues: issue series_id is good_series and             #
# indicia_publisher_id is good_indicia_publishers                              #
# #############################################################################;

DROP TABLE IF EXISTS {{sourceSchema}}.good_issue;

CREATE TABLE {{sourceSchema}}.good_issue AS
SELECT *
FROM {{sourceSchema}}.gcd_issue
WHERE series_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_series
  )
AND (indicia_publisher_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_indicia_publishers
    )
OR indicia_publisher_id IS NULL);

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_issues;

CREATE TABLE {{sourceSchema}}.migrate_issues AS
SELECT new.*
FROM {{sourceSchema}}.good_issue new
  LEFT JOIN {{targetSchema}}.gcd_issue old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_issue
  )
  OR new.modified != old.modified;

# ##############################################################################
# Create views of 'good' items: story issue_id is good_issue and               #
# story type_id is 6 or 19                                                     #
# #############################################################################;

DROP TABLE IF EXISTS {{sourceSchema}}.good_story;

CREATE TABLE {{sourceSchema}}.good_story AS
SELECT *
FROM {{sourceSchema}}.gcd_story
WHERE issue_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_issue
  )
  AND type_id IN (6, 19);

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_stories;

CREATE TABLE {{sourceSchema}}.migrate_stories AS
SELECT new.*
FROM {{sourceSchema}}.good_story new
  LEFT JOIN {{targetSchema}}.gcd_story old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_story
  )
  OR new.modified != old.modified;

# ##############################################################################
# Create views of 'good' items: story credit story_id is good_story            #
# #############################################################################;

DROP TABLE IF EXISTS {{sourceSchema}}.good_story_credit;

CREATE TABLE {{sourceSchema}}.good_story_credit AS
SELECT *
FROM {{sourceSchema}}.gcd_story_credit
WHERE story_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_story
  );

DROP TABLE IF EXISTS {{sourceSchema}}.migrate_story_credits;

CREATE TABLE {{sourceSchema}}.migrate_story_credits AS
SELECT new.*
FROM {{sourceSchema}}.good_story_credit new
  LEFT JOIN {{targetSchema}}.gcd_story_credit old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM {{targetSchema}}.gcd_story_credit
  )
  OR new.modified != old.modified;

# ###########################################################################
# # Add extracted item table constraints if not exist                       #
# ##########################################################################;
# SELECT COUNT(*)
# INTO @exist
# FROM information_schema.statistics
# WHERE table_schema = '{{sourceSchema}}'
#   AND table_name = 'm_character'
#   AND column_name = 'name';
# 
# SET @query_add_index_m_character_name =
#     IF(@exist <= 0, 'CREATE INDEX m_character_name ON {{sourceSchema}}.m_character (name)',
#         'SELECT \'Index Exists\' status');
# 
# PREPARE stmt FROM @query_add_index_m_character_name;
# EXECUTE stmt;
# 
# SELECT COUNT(*)
# INTO @exist
# FROM information_schema.statistics
# WHERE table_schema = '{{sourceSchema}}'
#   AND table_name = 'm_character'
#   AND column_name = 'alter_ego';
# 
# SET @query_add_index_m_character_alter_ego =
#         IF(@exist <= 0, 'CREATE INDEX m_character_alter_ego ON {{sourceSchema}}.m_character (alter_ego)',
#            'SELECT \'Index Exists\' status');
# 
# PREPARE stmt FROM @query_add_index_m_character_alter_ego;
# EXECUTE stmt;
# 
# SELECT COUNT(*)
# INTO @exist
# FROM information_schema.statistics
# WHERE table_schema = '{{sourceSchema}}'
#   AND table_name = 'm_character_appearance'
#   AND index_name = 'detail_2';
# 
# SET @query_add_index_m_character_alter_ego =
#         IF(@exist <= 0, 'ALTER TABLE {{sourceSchema}}.m_character_appearance ADD INDEX (details, character_id, story_id, notes)',
#            'SELECT \'Index Exists\' status');
# 
# PREPARE stmt FROM @query_add_index_m_character_alter_ego;
# EXECUTE stmt;
# 
# 
# CREATE INDEX m_character_appearance_notes ON {{sourceSchema}}.m_character_appearance (notes);
# CREATE INDEX m_character_appearance_details ON {{sourceSchema}}.m_character_appearance (details);
# 
# ALTER TABLE {{sourceSchema}}.m_character_appearance
#     ADD FOREIGN KEY (story_id) REFERENCES gcd_story (id);
# ALTER TABLE {{sourceSchema}}.m_character_appearance
#     ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id);
# ALTER TABLE {{sourceSchema}}.m_character_appearance
#     ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id);

