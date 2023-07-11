package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.db.QueryExecutor
import java.sql.Connection

class DBInitRemoveRecords(
    private val queryExecutor: QueryExecutor,
    private val targetSchema: String,
    private val conn: Connection
) {
    fun deleteGcdBiblioEntryFromBadStories() {
        val query = """DELETE gbe
            FROM $targetSchema.gcd_biblio_entry gbe
            WHERE gbe.story_ptr_id IN (SELECT id FROM $targetSchema.bad_stories);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdStoryCreditFromBadStories() {
        val query = """DELETE gsc
            FROM $targetSchema.gcd_story_credit gsc
            WHERE gsc.story_id IN (SELECT id FROM $targetSchema.bad_stories);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdStoryFeatureObjectFromBadStories() {
        val query = """DELETE gsfo
            FROM $targetSchema.gcd_story_feature_object gsfo
            WHERE gsfo.story_id IN (SELECT id FROM $targetSchema.bad_stories);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdStoryFeatureLogoFromBadStories() {
        val query = """DELETE gsfl
            FROM $targetSchema.gcd_story_feature_logo gsfl
            WHERE gsfl.story_id IN (SELECT id FROM $targetSchema.bad_stories);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdIssueCreditFromBadIssues() {
        val query = """DELETE gic
            FROM $targetSchema.gcd_issue_credit gic
            WHERE gic.issue_id IN (SELECT id FROM $targetSchema.bad_issues);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdIssueIndiciaPrinterFromBadIssues() {
        val query = """DELETE giip
            FROM $targetSchema.gcd_issue_indicia_printer giip
            WHERE giip.issue_id IN (SELECT id FROM $targetSchema.bad_issues);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdSeriesBondFromBadIssues() {
        val query = """DELETE gsb
            FROM $targetSchema.gcd_series_bond gsb
            WHERE gsb.origin_issue_id IN (SELECT id FROM $targetSchema.bad_issues)
            OR gsb.target_issue_id IN (SELECT id FROM $targetSchema.bad_issues);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdReprintFromBadIssues() {
        val query = """DELETE gr
            FROM $targetSchema.gcd_reprint gr
            WHERE gr.origin_id IN (SELECT id FROM $targetSchema.bad_issues)
            OR gr.target_id IN (SELECT id FROM $targetSchema.bad_issues);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdStoryFromBadIssues() {
        val query = """DELETE gs
            FROM $targetSchema.gcd_story gs
            WHERE gs.issue_id IN (SELECT id FROM $targetSchema.bad_issues);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }


    /**
     * This is so that we can safely remove any issues from bad_issues that are
     * referenced as a variant_of_id in gcd_issue.
     */
    fun setVariantOfIdToNullIfItIsBad() {
        val query = """UPDATE $targetSchema.gcd_issue gi
            JOIN $targetSchema.gcd_issue gi2 ON gi.variant_of_id = gi2.id
            JOIN gcd_series gs ON gi2.series_id = gs.id
            SET gi.variant_of_id = NULL
            WHERE gi2.id in (SELECT id FROM $targetSchema.bad_issues)
            OR gi2.series_id IN (SELECT id FROM $targetSchema.bad_series)
            OR gs.publisher_id IN (SELECT id FROM $targetSchema.bad_publishers)
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }


    /**
     * This is so that we can safely remove any series from bad_series that are
     * referenced as a variant_of_id in gcd_series.
     */
    fun setFirstLastIssueToNullIfItIsBad() {
        val query = """UPDATE $targetSchema.gcd_series gs
            SET gs.first_issue_id = NULL, gs.last_issue_id = NULL
            WHERE gs.publisher_id IN (SELECT id FROM $targetSchema.bad_publishers)
            OR (gs.country_id != 225 OR gs.language_id != 25 OR gs.year_began < 1900)
            OR gs.last_issue_id IN (SELECT id FROM $targetSchema.gcd_issue WHERE series_id IN (SELECT id FROM $targetSchema.bad_series))
            OR gs.first_issue_id IN (SELECT id FROM $targetSchema.gcd_issue WHERE series_id IN (SELECT id FROM $targetSchema.bad_series));
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdIssueFromBadSeries() {
        val query = """DELETE gi
            FROM $targetSchema.gcd_issue gi
            WHERE gi.series_id IN (SELECT id FROM $targetSchema.bad_series);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdSeriesBondFromBadSeries() {
        val query = """DELETE gsb
            FROM $targetSchema.gcd_series_bond gsb
            WHERE gsb.origin_id IN (SELECT id FROM $targetSchema.bad_series)
            OR gsb.target_id IN (SELECT id FROM $targetSchema.bad_series);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    /**
     * Remove any series that:
     * - country_id != 225
     * - language_id != 25
     * - year_began < 1900
     * - publisher_id references a publisher that meets the criteria to be in
     *   bad_publishers (Cannot reference bad_publishers because its definition
     *   relies upon gcd_series)
     */
    fun deleteGcdSeriesFromBadPublishers() {
        val query = """DELETE gs
            FROM $targetSchema.gcd_series gs
            WHERE gs.publisher_id IN (
                SELECT id 
                FROM $targetSchema.gcd_publisher
                WHERE country_id != 225)
            OR (gs.country_id != 225 OR gs.language_id != 25 OR gs.year_began < 1900);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdIssueFromBadIndiciaPublishers() {
        val query = """ DELETE gi
        FROM $targetSchema.gcd_issue gi
        WHERE gi . indicia_publisher_id IN(SELECT id FROM $targetSchema.bad_indicia_publishers);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdIndiciaPublisherFromBadPublishers() {
        val query = """ DELETE gip
        FROM $targetSchema.gcd_indicia_publisher gip
        WHERE gip . publisher_id IN(SELECT id FROM $targetSchema.bad_publishers);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdBrandEmblemGroupFromBadBrandGroups() {
        val query = """ DELETE gbeg
        FROM $targetSchema.gcd_brand_emblem_group gbeg
        WHERE gbeg . brandgroup_id IN(SELECT id FROM $targetSchema.bad_brand_groups);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdBrandGroupFromBadPublishers() {
        val query = """ DELETE gbg
        FROM $targetSchema.gcd_brand_group gbg
        WHERE gbg . parent_id IN(SELECT id FROM $targetSchema.bad_publishers);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdBrandUseFromBadPublishers() {
        val query = """ DELETE gbu
        FROM $targetSchema.gcd_brand_use gbu
        WHERE gbu . publisher_id IN(SELECT id FROM $targetSchema.bad_publishers);
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }

    fun deleteGcdPublisherFromBadPublishers() {
        val query = """ DELETE gp
        FROM $targetSchema.gcd_publisher gp
        WHERE gp . country_id != 225 OR gp.year_began < 1900;
        """.trimIndent()

        queryExecutor.executeSqlStatement(query, conn)
    }
}