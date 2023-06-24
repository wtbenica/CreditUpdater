#     This script will create a set of views that can be used to remove the "unwanted"
#     publishers, series, issues, stories, indicia publishers, and brand groups from
#     the database.  The views are:
#
#     bad_publishers:  A list of publishers that are not from the US or Canada, or
#                      that started publishing before 1900.
#
#     bad_series:  A list of series that are not from the US or Canada, or that are
#                  not in English, or that are published by a publisher in the
#                  'bad_publishers' view.
#
#     bad_issues:  A list of issues that are in a series in the 'bad_series' view.
#
#     bad_stories:  A list of stories that are in an issue in the 'bad_issues' view.
#
#     bad_indicia_publishers:  A list of indicia publishers that are in a series in
#                              the 'bad_series' view.
#
#     bad_brand_groups:  A list of brand groups that are published by a publisher
#                        in the 'bad_publishers' view.;


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
   OR gs.publisher_id IN (SELECT id FROM bad_publishers));


 CREATE OR REPLACE VIEW bad_issues
 AS
 (
 SELECT gi.id
 FROM gcd_issue gi
 WHERE gi.series_id IN (SELECT id FROM bad_series));

 CREATE OR REPLACE VIEW bad_stories
 AS
 (
 SELECT gs.id
 FROM gcd_story gs
 WHERE gs.issue_id IN (SELECT id FROM bad_issues));

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
