# ###########################################################################
# Create views of 'good' items: publisher is US && > 1900                   #
# ##########################################################################;

DROP TABLE IF EXISTS <schema>.good_publishers;
CREATE TABLE <schema>.good_publishers AS
SELECT *
FROM <schema>.gcd_publisher
WHERE country_id = 225
  AND year_began >= 1900;

DROP TABLE IF EXISTS <schema>.publishers_to_migrate;
CREATE TABLE <schema>.publishers_to_migrate AS
SELECT new.*
FROM <schema>.good_publishers new
         LEFT JOIN gcdb2.gcd_publisher old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_publisher
)
   OR new.modified != old.modified;

DROP TABLE IF EXISTS <schema>.good_series;
CREATE TABLE <schema>.good_series AS
SELECT *
FROM <schema>.gcd_series
WHERE publisher_id IN (
    SELECT id
    FROM <schema>.good_publishers
)
  AND country_id = 225
  AND language_id = 25;

DROP TABLE IF EXISTS <schema>.series_to_migrate;
CREATE TABLE <schema>.series_to_migrate AS
SELECT new.*
FROM <schema>.good_series new
         LEFT JOIN gcdb2.gcd_series old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_series
)
   OR new.modified != old.modified;

DROP TABLE IF EXISTS <schema>.good_issue;
CREATE TABLE <schema>.good_issue AS
SELECT *
FROM <schema>.gcd_issue
WHERE series_id IN (
    SELECT id
    FROM <schema>.good_series
);

DROP TABLE IF EXISTS <schema>.issues_to_migrate;
CREATE TABLE <schema>.issues_to_migrate AS
SELECT new.*
FROM <schema>.good_issue new
         LEFT JOIN gcdb2.gcd_issue old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_issue
)
   OR new.modified != old.modified;

DROP TABLE <schema>.good_story;
CREATE TABLE <schema>.good_story AS
SELECT *
FROM <schema>.gcd_story
WHERE issue_id IN (
    SELECT id
    FROM <schema>.good_issue
)
  AND type_id IN (6, 19);

DROP TABLE IF EXISTS <schema>.stories_to_migrate;
CREATE TABLE <schema>.stories_to_migrate AS
SELECT new.*
FROM <schema>.good_story new
         LEFT JOIN gcdb2.gcd_story old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_story
)
   OR new.modified != old.modified;

DROP TABLE IF EXISTS <schema>.good_story_credit;
CREATE TABLE <schema>.good_story_credit AS
SELECT *
FROM <schema>.gcd_story_credit
WHERE story_id IN (
    SELECT id
    FROM <schema>.good_story
);

DROP TABLE IF EXISTS <schema>.story_credits_to_migrate;
CREATE TABLE <schema>.story_credits_to_migrate AS
SELECT new.*
FROM <schema>.good_story_credit new
         LEFT JOIN gcdb2.gcd_story_credit old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_story_credit
)
   OR new.modified != old.modified;
