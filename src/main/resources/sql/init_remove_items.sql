# ------------------------------------------------------------------------------------------
# This script is used to shrink the database by removing all data for publishers that are
# not in the good_publishers table.  It is used to shrink the database after the
# good_publishers table has been updated.
# -----------------------------------------------------------------------------------------;

 DELETE gsc
 FROM gcd_story_credit gsc
 WHERE gsc.story_id IN (SELECT id FROM bad_stories);

 DELETE
 FROM gcd_reprint
 WHERE origin_id IN (SELECT id FROM bad_stories)
 OR target_id IN (SELECT id FROM bad_stories);

 DELETE gic
 FROM gcd_issue_credit gic
 WHERE gic.issue_id IN (SELECT id FROM bad_issues);

 DELETE
 FROM gcd_series_bond
 WHERE origin_issue_id IN (SELECT id FROM bad_issues)
    OR target_issue_id IN (SELECT id FROM bad_issues);

 DELETE
 FROM gcd_reprint
 WHERE origin_issue_id IN (SELECT id FROM bad_issues)
 OR target_issue_id IN (SELECT id FROM bad_issues);

 DELETE
 FROM gcd_story
 WHERE issue_id IN (SELECT id FROM bad_issues);

 UPDATE gcd_issue
     JOIN gcd_series gs ON gcd_issue.series_id = gs.id
 SET variant_of_id = NULL
 WHERE gcd_issue.series_id IN (SELECT id FROM bad_series)
    OR gs.publisher_id IN (SELECT id FROM bad_publishers);

 # ------------------------------------------------------------------------------------------
 # This is a messy one. It sets the series first and last issue ids to null for issues that are about to be deleted.
 # It got messy because here were two issues whose ids were the last_issue_id to series whose ids
 # didn't match the issues' series_ids.
 # -----------------------------------------------------------------------------------------;

 UPDATE gcd_series
 SET first_issue_id = NULL,
     last_issue_id  = NULL
 WHERE publisher_id IN (SELECT id FROM bad_publishers)
    OR country_id != 225
    OR language_id != 25
    OR last_issue_id IN (
     SELECT id
     FROM (
              SELECT gi.id
              FROM gcd_issue gi
              WHERE gi.series_id IN (SELECT id FROM bad_series)) AS t)
    OR first_issue_id IN (
     SELECT id
     FROM (
              SELECT gi.id
              FROM gcd_issue gi
              WHERE gi.series_id IN (SELECT id FROM bad_series)) AS u);

 DELETE
 FROM gcd_issue
 WHERE series_id IN (SELECT id FROM bad_series)
 OR indicia_publisher_id IN (SELECT id FROM bad_indicia_publishers);

 DELETE
 FROM gcd_series_bond
 WHERE origin_id IN (SELECT id FROM bad_series)
    OR target_id IN (SELECT id FROM bad_series);

 DELETE
 FROM gcd_series
 WHERE country_id != 225
    OR language_id != 25
    OR year_began < 1900
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
 FROM gcd_publisher
 WHERE country_id != 225
 OR id NOT IN (
    SELECT DISTINCT gp.id
    FROM gcd_publisher gp
        INNER JOIN gcd_series gs ON gp.id = gs.publisher_id
    WHERE gs.year_began >= 1900
 );