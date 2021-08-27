SELECT '2021-01-31 00:00'
INTO @last_update;

BEGIN;
REPLACE gcdb2.stddata_country
SELECT new.*
FROM new_gcd_dump.stddata_country new
         LEFT JOIN gcdb2.stddata_country old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.stddata_country
)
   OR new.code != old.code
   OR new.name != old.name;
COMMIT;


BEGIN;
REPLACE gcdb2.stddata_language
SELECT new.*
FROM new_gcd_dump.stddata_language new
         LEFT JOIN gcdb2.stddata_language old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.stddata_language
)
   OR new.code != old.code
   OR new.name != old.name
   OR new.native_name != old.native_name;
COMMIT;


BEGIN;
REPLACE gcdb2.stddata_date
SELECT new.*
FROM new_gcd_dump.stddata_date new
         LEFT JOIN gcdb2.stddata_date old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.stddata_date
)
   OR new.day != old.day
   OR new.day_uncertain != old.day_uncertain
   OR new.month != old.month
   OR new.month_uncertain != old.month_uncertain
   OR new.year != old.year
   OR new.year_uncertain != old.year_uncertain;
COMMIT;

BEGIN;
REPLACE gcdb2.gcd_series_publication_type
SELECT new.*
FROM new_gcd_dump.gcd_series_publication_type new
         LEFT JOIN gcdb2.gcd_series_publication_type old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_series_publication_type
)
   OR new.name != old.name
   OR new.notes != old.notes;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE gcdb2.gcd_brand
SELECT new.*
FROM new_gcd_dump.gcd_brand new
         LEFT JOIN gcdb2.gcd_brand old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_brand
)
   OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
REPLACE gcdb2.gcd_story_type
SELECT new.*
FROM new_gcd_dump.gcd_story_type new
         LEFT JOIN gcdb2.gcd_story_type old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_story_type
)
   OR new.name != old.name
   OR new.sort_code != old.sort_code;
COMMIT;

BEGIN;
REPLACE gcdb2.gcd_name_type
SELECT new.*
FROM new_gcd_dump.gcd_name_type new
         LEFT JOIN gcdb2.gcd_name_type old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_name_type
)
   OR new.description != old.description
   OR new.type != old.type;
COMMIT;

BEGIN;
REPLACE gcdb2.stddata_script
SELECT new.*
FROM new_gcd_dump.stddata_script new
         LEFT JOIN gcdb2.stddata_script old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.stddata_script
)
   OR new.name != old.name
   OR new.code != old.code
   OR new.number != old.number;
COMMIT;

BEGIN;
REPLACE gcdb2.gcd_credit_type
SELECT new.*
FROM new_gcd_dump.gcd_credit_type new
         LEFT JOIN gcdb2.gcd_credit_type old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_credit_type
)
   OR new.name != old.name
   OR new.sort_code != old.sort_code;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;

REPLACE gcdb2.gcd_publisher
SELECT *
FROM new_gcd_dump.publishers_to_migrate;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE series_fk_first_issues
SELECT ngi.*
FROM new_gcd_dump.gcd_issue ngi
WHERE ngi.id IN (
    SELECT stm.first_issue_id
    FROM new_gcd_dump.series_to_migrate stm
)
  AND ngi.id NOT IN (
    SELECT ogi.id
    FROM gcdb2.gcd_issue ogi
);

REPLACE gcdb2.gcd_issue
SELECT *
FROM new_gcd_dump.gcd_issue ngi
WHERE ngi.id IN (
    SELECT variant_of_id
    FROM series_fk_first_issues
);

REPLACE gcdb2.gcd_issue
SELECT *
FROM series_fk_first_issues;

REPLACE gcdb2.gcd_series
SELECT *
FROM new_gcd_dump.series_to_migrate;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;


BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE gcdb2.gcd_indicia_publisher
SELECT new.*
FROM new_gcd_dump.gcd_indicia_publisher new
         LEFT JOIN gcdb2.gcd_indicia_publisher old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_indicia_publisher
)
   OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE issues_fk_variant_ofs
SELECT *
FROM new_gcd_dump.issues_to_migrate
WHERE variant_of_id NOT IN (
    SELECT id
    FROM gcdb2.gcd_issue
);


REPLACE gcdb2.gcd_issue
SELECT *
FROM issues_fk_variant_ofs;

REPLACE gcdb2.gcd_issue
SELECT *
FROM new_gcd_dump.issues_to_migrate;

SET foreign_key_checks = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE gcdb2.gcd_story
SELECT *
FROM new_gcd_dump.stories_to_migrate;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE gcdb2.gcd_creator
SELECT new.*
FROM new_gcd_dump.gcd_creator new
         LEFT JOIN gcdb2.gcd_creator old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_creator
)
   OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE gcdb2.gcd_creator_name_detail
SELECT new.*
FROM new_gcd_dump.gcd_creator_name_detail new
         LEFT JOIN gcdb2.gcd_creator_name_detail old ON new.id = old.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_creator_name_detail
)
   OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;


BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE gcdb2.gcd_creator_signature
SELECT new.*
FROM new_gcd_dump.gcd_creator_signature new
         LEFT JOIN gcdb2.gcd_creator_signature old ON old.id = new.id
WHERE new.id NOT IN (
    SELECT id
    FROM gcdb2.gcd_creator_signature
)
   OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE gcdb2.gcd_story_credit
SELECT *
FROM new_gcd_dump.story_credits_to_migrate;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

INSERT INTO gcdb2.m_story_credit(created, modified, deleted, is_credited, is_signed, uncertain, signed_as, credited_as,
                                 credit_name, creator_id, credit_type_id, story_id, signature_id, issue_id, series_id)
SELECT mc1.created,
       mc1.modified,
       mc1.deleted,
       mc1.is_credited,
       mc1.is_signed,
       mc1.uncertain,
       mc1.signed_as,
       mc1.credited_as,
       mc1.credit_name,
       mc1.creator_id,
       mc1.credit_type_id,
       mc1.story_id,
       mc1.signature_id,
       mc1.issue_id,
       mc1.series_id
FROM new_gcd_dump.m_story_credit mc1
WHERE (
          SELECT COUNT(*)
          FROM gcdb2.m_story_credit mc2
          WHERE mc1.story_id = mc2.story_id
            AND mc1.creator_id = mc2.creator_id
            AND mc1.credit_type_id = mc2.credit_type_id
      ) = 0;

INSERT INTO gcdb2.m_character(name, alter_ego, publisher_id)
SELECT mc1.name, mc1.alter_ego, mc1.publisher_id
FROM new_gcd_dump.m_character mc1
WHERE (
          SELECT COUNT(*)
          FROM gcdb2.m_character mc2
          WHERE mc1.name = mc2.name
            AND mc1.alter_ego = mc2.alter_ego
            AND mc1.publisher_id = mc2.publisher_id
      ) = 0;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE new_gcd_dump.m_character
    ADD COLUMN new_id INTEGER;

UPDATE new_gcd_dump.m_character new, gcdb2.m_character old
SET new.new_id = old.id
WHERE new.name = old.name
  AND (new.alter_ego = old.alter_ego
    OR (
               new.alter_ego IS NULL
               AND old.alter_ego IS NULL
           ))
  AND new.publisher_id = old.publisher_id;

UPDATE new_gcd_dump.m_character_appearance, new_gcd_dump.m_character
SET character_id = m_character.new_id
WHERE character_id = m_character.id;

UPDATE new_gcd_dump.m_character
SET id = new_id
WHERE id != m_character.new_id;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

# Remove Duplicates
DELETE nca
FROM new_gcd_dump.m_character_appearance nca
         INNER JOIN gcdb2.m_character_appearance oca
                    ON oca.character_id = nca.character_id
                        AND oca.story_id = nca.story_id
                        AND (
                               (
                                       oca.notes IS NULL
                                       AND nca.notes IS NULL
                                   )
                               OR (
                                   oca.notes = nca.notes
                                   )
                           )
                        AND (
                               (
                                       oca.details IS NULL
                                       AND nca.details IS NULL
                                   )
                               OR (
                                   oca.details = nca.details
                                   )
                           )
                        AND (
                               (
                                       oca.membership IS NULL
                                       AND nca.membership IS NULL
                                   )
                               OR (
                                   oca.membership = nca.membership
                                   )
                           );

DROP TEMPORARY TABLE mca_updates_only;
CREATE TEMPORARY TABLE mca_updates_only AS
SELECT *
FROM new_gcd_dump.m_character_appearance new
WHERE (
          SELECT COUNT(*)
          FROM gcdb2.m_character_appearance old
          WHERE new.character_id = old.character_id
            AND new.story_id = old.story_id
            AND (
                  (
                          new.details IS NOT NULL
                          AND old.details IS NULL
                      )
                  OR (
                          new.details IS NOT NULL
                          AND old.details IS NOT NULL
                          AND new.details != old.details
                      )
                  OR (
                          new.notes IS NOT NULL
                          AND old.notes IS NULL
                      )
                  OR (
                          new.notes IS NOT NULL
                          AND old.notes IS NOT NULL
                          AND new.notes != old.notes
                      )
                  OR (
                          new.membership IS NOT NULL
                          AND old.membership IS NULL)
                  OR (
                          new.membership IS NOT NULL
                          AND old.membership IS NOT NULL
                          AND new.membership != old.membership
                      )
              )
      ) > 0
ORDER BY story_id, character_id, details, notes, membership;

UPDATE mca_updates_only upd
SET id = (
    SELECT id
    FROM gcdb2.m_character_appearance mca
    WHERE mca.story_id = upd.story_id
      AND mca.character_id = upd.character_id
    ORDER BY story_id, character_id, details, notes, membership
    LIMIT 1
);

REPLACE INTO gcdb2.m_character_appearance
SELECT *
FROM mca_updates_only;

INSERT INTO gcdb2.m_character_appearance(details, character_id, story_id, notes, membership, issue_id, series_id)
SELECT mc1.details, mc1.character_id, mc1.story_id, mc1.notes, mc1.membership, mc1.issue_id, mc1.series_id
FROM new_gcd_dump.m_character_appearance mc1
WHERE mc1.story_id IN (
    SELECT id
    FROM new_gcd_dump.good_story
)
  AND (
          SELECT COUNT(*)
          FROM gcdb2.m_character_appearance mc2
          WHERE mc1.character_id = mc2.character_id
            AND mc1.story_id = mc2.story_id
      ) = 0;

