# Migrate static table data;
BEGIN;
REPLACE {{targetSchema}}.stddata_country
SELECT new.*
FROM {{sourceSchema}}.stddata_country new
    LEFT JOIN {{targetSchema}}.stddata_country old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.stddata_country
    )
    OR new.code != old.code
    OR new.name != old.name;
COMMIT;

BEGIN;
REPLACE {{targetSchema}}.stddata_language
SELECT new.*
FROM {{sourceSchema}}.stddata_language new
    LEFT JOIN {{targetSchema}}.stddata_language old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.stddata_language
    )
    OR new.modified != old.modified;
COMMIT;

BEGIN;

REPLACE {{targetSchema}}.stddata_date
SELECT new.*
FROM {{sourceSchema}}.stddata_date new
    LEFT JOIN {{targetSchema}}.stddata_date old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.stddata_date
    )
    OR new.modified != old.modified;
COMMIT;

BEGIN;
REPLACE {{targetSchema}}.gcd_series_publication_type
SELECT new.*
FROM {{sourceSchema}}.gcd_series_publication_type new
    LEFT JOIN {{targetSchema}}.gcd_series_publication_type old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_series_publication_type
    )
    OR new.modified != old.modified;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_brand
SELECT new.*
FROM {{sourceSchema}}.gcd_brand new
    LEFT JOIN {{targetSchema}}.gcd_brand old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_brand
    )
    OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
REPLACE {{targetSchema}}.gcd_story_type
SELECT new.*
FROM {{sourceSchema}}.gcd_story_type new
    LEFT JOIN {{targetSchema}}.gcd_story_type old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_story_type
    )
    OR new.modified != old.modified;
COMMIT;

BEGIN;
REPLACE {{targetSchema}}.gcd_name_type
SELECT new.*
FROM {{sourceSchema}}.gcd_name_type new
    LEFT JOIN {{targetSchema}}.gcd_name_type old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_name_type
    )
    OR new.modified != old.modified;
COMMIT;

BEGIN;
REPLACE {{targetSchema}}.stddata_script
SELECT new.*
FROM {{sourceSchema}}.stddata_script new
    LEFT JOIN {{targetSchema}}.stddata_script old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.stddata_script
    )
    OR new.modified != old.modified;
COMMIT;

BEGIN;
REPLACE {{targetSchema}}.gcd_credit_type
SELECT new.*
FROM {{sourceSchema}}.gcd_credit_type new
    LEFT JOIN {{targetSchema}}.gcd_credit_type old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_credit_type
    )
    OR new.modified != old.modified;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_publisher
SELECT *
FROM {{sourceSchema}}.migrate_publishers;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

# This transaction migrates data from the source schema gcd_series table to the target schema gcd_series tables.
# First it copies over any gcd_issues that are the first issue of a series or a variant of a first issue, then
# it copies over the gcd_series table, and finally it copies over the gcd_issues that are not first issues or
# variants of first issues.;
BEGIN;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE series_fk_first_issues
SELECT ngi.*
FROM {{sourceSchema}}.gcd_issue ngi
WHERE ngi.id IN (
        SELECT stm.first_issue_id
        FROM {{sourceSchema}}.migrate_series stm
    )
    AND ngi.id NOT IN (
        SELECT ogi.id
        FROM {{targetSchema}}.gcd_issue ogi
    );

REPLACE {{targetSchema}}.gcd_issue
SELECT *
FROM {{sourceSchema}}.gcd_issue ngi
WHERE ngi.id IN (
        SELECT variant_of_id
        FROM series_fk_first_issues
    );

REPLACE {{targetSchema}}.gcd_issue
SELECT *
FROM series_fk_first_issues;

REPLACE {{targetSchema}}.gcd_series
SELECT *
FROM {{sourceSchema}}.migrate_series;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_indicia_publisher
SELECT *
FROM {{sourceSchema}}.migrate_indicia_publishers;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
CREATE TEMPORARY TABLE issues_fk_variant_ofs
SELECT *
FROM {{sourceSchema}}.migrate_issues
WHERE variant_of_id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_issue
    );
REPLACE {{targetSchema}}.gcd_issue
SELECT *
FROM issues_fk_variant_ofs;
REPLACE {{targetSchema}}.gcd_issue
SELECT *
FROM {{sourceSchema}}.migrate_issues;
SET foreign_key_checks = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_series_bond
SELECT sb1.*
FROM {{sourceSchema}}.gcd_series_bond sb1
WHERE sb1.origin_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_series
)
AND sb1.target_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_series
)
AND sb1.origin_issue_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_issue
)
AND sb1.target_issue_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_issue
)
AND (
    sb1.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_series_bond
    )
    OR sb1.modified != (
        SELECT modified
        FROM {{targetSchema}}.gcd_series_bond sb2
        WHERE sb1.id = sb2.id
    )
);
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_story
SELECT *
FROM {{sourceSchema}}.migrate_stories;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_creator
SELECT new.*
FROM {{sourceSchema}}.gcd_creator new
    LEFT JOIN {{targetSchema}}.gcd_creator old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_creator
    )
    OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_creator_name_detail
SELECT new.*
FROM {{sourceSchema}}.gcd_creator_name_detail new
    LEFT JOIN {{targetSchema}}.gcd_creator_name_detail old ON new.id = old.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_creator_name_detail
    )
    OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_creator_signature
SELECT new.*
FROM {{sourceSchema}}.gcd_creator_signature new
    LEFT JOIN {{targetSchema}}.gcd_creator_signature old ON old.id = new.id
WHERE new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_creator_signature
    )
    OR new.modified != old.modified;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_story_credit
SELECT *
FROM {{sourceSchema}}.migrate_story_credits;
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_reprint
SELECT new.*
FROM {{sourceSchema}}.gcd_reprint new
    LEFT JOIN {{targetSchema}}.gcd_reprint old ON new.id = old.id
WHERE new.origin_id in (
    SELECT id
    FROM {{sourceSchema}}.good_story
)
AND new.target_id in (
    SELECT id
    FROM {{sourceSchema}}.good_story
)
AND new.origin_issue_id in (
    SELECT id
    FROM {{sourceSchema}}.good_issue
)
AND new.target_issue_id in (
    SELECT id
    FROM {{sourceSchema}}.good_issue
)
AND (new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_reprint
    )
    OR new.modified != old.modified);
SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
REPLACE {{targetSchema}}.gcd_issue_credit
SELECT new.*
FROM {{sourceSchema}}.gcd_issue_credit new
    LEFT JOIN {{targetSchema}}.gcd_issue_credit old ON new.id = old.id
WHERE new.issue_id IN (
    SELECT id
    FROM {{sourceSchema}}.good_issue
)
AND (
    new.id NOT IN (
        SELECT id
        FROM {{targetSchema}}.gcd_issue_credit
    )
    OR new.modified != old.modified
);

INSERT INTO {{targetSchema}}.m_story_credit(
        created,
        modified,
        deleted,
        is_credited,
        is_signed,
        uncertain,
        signed_as,
        credited_as,
        credit_name,
        creator_id,
        credit_type_id,
        story_id,
        signature_id,
        issue_id,
        series_id
    )
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
FROM {{sourceSchema}}.m_story_credit mc1
WHERE (
        SELECT COUNT(*)
        FROM {{targetSchema}}.m_story_credit mc2
        WHERE mc1.story_id = mc2.story_id
            AND mc1.creator_id = mc2.creator_id
            AND mc1.credit_type_id = mc2.credit_type_id
    ) = 0;

INSERT INTO {{targetSchema}}.m_character(name, alter_ego, publisher_id)
SELECT mc1.name,
    mc1.alter_ego,
    mc1.publisher_id
FROM {{sourceSchema}}.m_character mc1
WHERE (
        SELECT COUNT(*)
        FROM {{targetSchema}}.m_character mc2
        WHERE mc1.name = mc2.name
            AND mc1.alter_ego = mc2.alter_ego
            AND mc1.publisher_id = mc2.publisher_id
    ) = 0;

BEGIN;
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE {{sourceSchema}}.m_character
ADD COLUMN new_id INTEGER;
UPDATE {{sourceSchema}}.m_character new,
    {{targetSchema}}.m_character old
SET new.new_id = old.id
WHERE new.name = old.name
    AND (
        new.alter_ego = old.alter_ego
        OR (
            new.alter_ego IS NULL
            AND old.alter_ego IS NULL
        )
    )
    AND new.publisher_id = old.publisher_id;

UPDATE {{sourceSchema}}.m_character_appearance,
    {{sourceSchema}}.m_character
SET character_id = m_character.new_id
WHERE character_id = m_character.id;

UPDATE {{sourceSchema}}.m_character
SET id = new_id
WHERE id != m_character.new_id;

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;

# Remove Duplicates
DELETE nca
FROM {{sourceSchema}}.m_character_appearance nca
    INNER JOIN {{targetSchema}}.m_character_appearance oca ON oca.character_id = nca.character_id
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

DROP TEMPORARY TABLE IF EXISTS {{sourceSchema}}.mca_updates_only;

CREATE TEMPORARY TABLE {{sourceSchema}}.mca_updates_only AS
SELECT *
FROM {{sourceSchema}}.m_character_appearance new
WHERE (
        SELECT COUNT(*)
        FROM {{targetSchema}}.m_character_appearance old
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
                    AND old.membership IS NULL
                )
                OR (
                    new.membership IS NOT NULL
                    AND old.membership IS NOT NULL
                    AND new.membership != old.membership
                )
            )
    ) > 0
ORDER BY story_id,
    character_id,
    details,
    notes,
    membership;

UPDATE {{sourceSchema}}.mca_updates_only upd
SET id = (
        SELECT id
        FROM {{targetSchema}}.m_character_appearance mca
        WHERE mca.story_id = upd.story_id
            AND mca.character_id = upd.character_id
        ORDER BY story_id,
            character_id,
            details,
            notes,
            membership
        LIMIT 1
    );

REPLACE INTO {{targetSchema}}.m_character_appearance
SELECT *
FROM {{sourceSchema}}.mca_updates_only;

INSERT INTO {{targetSchema}}.m_character_appearance(
        details,
        character_id,
        story_id,
        notes,
        membership,
        issue_id,
        series_id
    )
SELECT mc1.details,
    mc1.character_id,
    mc1.story_id,
    mc1.notes,
    mc1.membership,
    mc1.issue_id,
    mc1.series_id
FROM {{sourceSchema}}.m_character_appearance mc1
WHERE mc1.story_id IN (
        SELECT id
        FROM {{sourceSchema}}.good_story
    )
    AND (
        SELECT COUNT(*)
        FROM {{targetSchema}}.m_character_appearance mc2
        WHERE mc1.character_id = mc2.character_id
            AND mc1.story_id = mc2.story_id
    ) = 0;