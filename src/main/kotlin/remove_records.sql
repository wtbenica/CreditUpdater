/*
    This script will limit the records in the database based on certain criteria
    that are shown in the 'bad_publishers' and 'bad_series' views
    */

 CREATE OR REPLACE VIEW bad_publishers
 AS
 (
 SELECT gp.id
 FROM gcd_publisher gp
 WHERE gp.country_id != 225
    OR gp.year_began < 1900 );

 CREATE OR REPLACE VIEW bad_series
 AS
 (
 SELECT gs.id
 FROM gcd_series gs
 WHERE gs.country_id != 225
    OR gs.language_id != 25
    OR gs.publisher_id IN (SELECT * FROM bad_publishers));


 CREATE OR REPLACE VIEW bad_issues
 AS
 (
 SELECT gi.id
 FROM gcd_issue gi
 WHERE gi.series_id IN (SELECT * FROM bad_series));

 CREATE OR REPLACE VIEW bad_stories
 AS
 (
 SELECT gs.id
 FROM gcd_story gs
 WHERE gs.issue_id IN (SELECT * FROM bad_issues));

 CREATE OR REPLACE VIEW bad_indicia_publishers
 AS
 (
 SELECT id
 FROM gcd_indicia_publisher
 WHERE parent_id IN (SELECT id FROM bad_series) );

 CREATE OR REPLACE VIEW bad_brand_groups
 AS
 (
 SELECT id
 FROM gcd_brand_group
 WHERE parent_id IN (SELECT id FROM bad_publishers) );

 DELETE
 FROM gcd_biblio_entry
 WHERE story_ptr_id IN (SELECT * FROM bad_stories);

 DELETE
 FROM gcd_story_credit gsc
 WHERE gsc.story_id IN (SELECT * FROM bad_stories);

 DELETE
 FROM gcd_reprint_to_issue
 WHERE origin_id IN (SELECT * FROM bad_stories);

 DELETE
 FROM gcd_reprint_from_issue
 WHERE target_id IN (SELECT * FROM bad_stories);

 DELETE
 FROM gcd_reprint
 WHERE origin_id IN (SELECT * FROM bad_stories)
    OR target_id IN (SELECT * FROM bad_stories);

 DELETE
 FROM gcd_story_feature_object
 WHERE story_id IN (SELECT * FROM bad_stories);

 DELETE
 FROM gcd_story_feature_logo
 WHERE story_id IN (SELECT * FROM bad_stories);

 DELETE
 FROM gcd_issue_credit gic
 WHERE gic.issue_id IN (SELECT * FROM bad_issues);

 DELETE
 FROM gcd_issue_indicia_printer
 WHERE issue_id IN (SELECT * FROM bad_issues);

 DELETE
 FROM gcd_series_bond
 WHERE origin_issue_id IN (SELECT * FROM bad_issues)
    OR target_issue_id IN (SELECT * FROM bad_issues);

 DELETE
 FROM gcd_issue_reprint
 WHERE origin_issue_id IN (SELECT * FROM bad_issues)
    OR target_issue_id IN (SELECT * FROM bad_issues);

 DELETE
 FROM gcd_reprint_from_issue
 WHERE origin_issue_id IN (SELECT * FROM bad_issues);

 DELETE
 FROM gcd_reprint_to_issue
 WHERE target_issue_id IN (SELECT * FROM bad_issues);

 DELETE
 FROM gcd_story
 WHERE issue_id IN (SELECT * FROM bad_issues);

 UPDATE gcd_issue
     JOIN gcd_series gs ON gcd_issue.series_id = gs.id
 SET variant_of_id = NULL
 WHERE gcd_issue.series_id IN (SELECT * FROM bad_series)
    OR gs.publisher_id IN (SELECT * FROM bad_publishers);

 # ------------------------------------------------------------------------------------------
 # This is a messy one. It sets the series first and last issue ids to null for issues that are about to be deleted.
 # It got messy because here were two issues whose ids were the last_issue_id to series whose ids
 # didn't match the issues' series_ids.

 UPDATE gcd_series
 SET first_issue_id = NULL,
     last_issue_id  = NULL
 WHERE publisher_id IN (SELECT * FROM bad_publishers)
    OR country_id != 225
    OR language_id != 25
    OR last_issue_id IN (
     SELECT id
     FROM (
              SELECT gi.id
              FROM gcd_issue gi
              WHERE gi.series_id IN (SELECT * FROM bad_series)) AS t)
    OR first_issue_id IN (
     SELECT id
     FROM (
              SELECT gi.id
              FROM gcd_issue gi
              WHERE gi.series_id IN (SELECT * FROM bad_series)) AS u);

 DELETE
 FROM gcd_issue
 WHERE series_id IN (SELECT * FROM bad_series);

 DELETE
 FROM gcd_series_bond
 WHERE origin_id IN (SELECT id FROM bad_series)
    OR target_id IN (SELECT id FROM bad_series);

 DELETE
 FROM gcd_series
 WHERE country_id != 225
    OR language_id != 25
    OR publisher_id IN (
     SELECT *
     FROM bad_publishers
 );

 DELETE
 FROM gcd_issue
 WHERE indicia_publisher_id IN (
     SELECT *
     FROM bad_indicia_publishers
 );

 DELETE
 FROM gcd_indicia_publisher
 WHERE parent_id IN (
     SELECT *
     FROM bad_publishers
 );

 DELETE
 FROM gcd_brand_emblem_group
 WHERE brandgroup_id IN (
     SELECT *
     FROM bad_brand_groups);

 DELETE
 FROM gcd_brand_group
 WHERE parent_id IN (
     SELECT *
     FROM bad_publishers
 );

 DELETE
 FROM gcd_brand_use
 WHERE publisher_id IN (
     SELECT *
     FROM bad_publishers
 );

 DELETE
 FROM gcd_publisher
 WHERE country_id != 225
    OR year_began < 1900;