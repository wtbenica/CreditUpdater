CREATE OR REPLACE VIEW `{{targetSchema}}`.bad_publishers AS (
        SELECT gp.id
        FROM gcd_publisher gp
        WHERE gp.country_id != 225
            OR gp.id NOT IN (
                SELECT DISTINCT gp.id
                FROM gcd_publisher gp
                    INNER JOIN gcd_series gs ON gp.id = gs.publisher_id
                WHERE gs.year_began >= 1900
            )
    );

CREATE OR REPLACE VIEW `{{targetSchema}}`.bad_indicia_publishers AS (
        SELECT gip.id
        FROM gcd_indicia_publisher gip
        WHERE gip.parent_id IN (
                SELECT id
                FROM bad_publishers
            )
    );

CREATE OR REPLACE VIEW `{{targetSchema}}`.bad_series AS (
        SELECT gs.id
        FROM gcd_series gs
        WHERE gs.country_id != 225
            OR gs.language_id != 25
            OR gs.year_began < 1900
            OR gs.publisher_id IN (
                SELECT id
                FROM bad_publishers
            )
    );

CREATE OR REPLACE VIEW `{{targetSchema}}`.bad_issues AS (
        SELECT gi.id
        FROM gcd_issue gi
        WHERE gi.series_id IN (
                SELECT id
                FROM bad_series
            )
        OR gi.indicia_publisher_id IN (
                SELECT id
                FROM bad_indicia_publishers
            )
    );

CREATE OR REPLACE VIEW `{{targetSchema}}`.bad_stories AS (
        SELECT gs.id
        FROM gcd_story gs
        WHERE gs.issue_id IN (
                SELECT id
                FROM bad_issues
            )
    );