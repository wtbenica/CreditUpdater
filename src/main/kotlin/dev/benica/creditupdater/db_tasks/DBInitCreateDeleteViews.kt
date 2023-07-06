package dev.benica.creditupdater.db_tasks

import dev.benica.creditupdater.db.QueryExecutor

/**
 * Class to create the delete views.
 *
 * These views are used to filter out data related to series that are not
 * related to series from the US, not in English or not from the 20th
 * century.
 *
 * @param queryExecutor the QueryExecutor to use to execute SQL statements
 * @param targetSchema the schema to create the views in
 */
class DBInitCreateDeleteViews(
    private val queryExecutor: QueryExecutor,
    private val targetSchema: String
) {

    /**
     * Create the delete views.
     *
     * These views are used to filter out data that is not related to series
     * from the US, not in English or not from the 20th century.
     */
    fun createDeleteViews() {
        createBadPublishersView()
        createBadSeriesView()
        createBadIssuesView()
        createBadStoriesView()
        createBadIndiciaPublishersView()
        createBadBrandGroupsView()
    }

    /**
     * Create the bad_publishers view.
     *
     * This view is used to filter out publishers that are not from the US or
     * are not from the 20th century.
     */
    internal fun createBadPublishersView() {
        val query =
            """CREATE OR REPLACE VIEW $targetSchema.bad_publishers AS (
                SELECT gp.id
                FROM gcd_publisher gp
                WHERE gp.country_id != 225
                OR gp.id NOT IN (
                    SELECT DISTINCT gp.id
                    FROM gcd_publisher gp
                    INNER JOIN gcd_series gs ON gp.id = gs.publisher_id
                    WHERE gs.year_began >= 1900
                )
            );""".trimIndent()

        queryExecutor.executeSqlStatement(query)
    }

    /**
     * Create the bad_series view.
     *
     * This view is used to filter out series that are not from the US or are
     * not in English or are not from the 20th century.
     */
    internal fun createBadSeriesView() {
        val query =
            """CREATE OR REPLACE VIEW $targetSchema.bad_series AS (
                SELECT gs.id
                FROM gcd_series gs
                WHERE gs.country_id != 225
                OR gs.language_id != 25
                OR gs.year_began < 1900
                OR gs.publisher_id IN (SELECT id FROM bad_publishers));""".trimIndent()

        queryExecutor.executeSqlStatement(query)
    }

    /**
     * Create the bad_issues view.
     *
     * This view is used to filter out issues that are from the bad_series
     * view.
     */
    internal fun createBadIssuesView() {
        val query =
            """CREATE OR REPLACE VIEW $targetSchema.bad_issues AS (
                SELECT gi.id
                FROM gcd_issue gi
                WHERE gi.series_id IN (SELECT id FROM bad_series));""".trimIndent()

        queryExecutor.executeSqlStatement(query)
    }

    /**
     * Create the bad_stories view.
     *
     * This view is used to filter out stories that are in issues from the
     * bad_issues view.
     */
    internal fun createBadStoriesView() {
        val query =
            """CREATE OR REPLACE VIEW $targetSchema.bad_stories AS (
                SELECT gs.id
                FROM gcd_story gs
                WHERE gs.issue_id IN (SELECT id FROM bad_issues));""".trimIndent()

        queryExecutor.executeSqlStatement(query)
    }

    /**
     * Create the bad_indicia_publishers view.
     *
     * This view is used to filter out indicia publishers that are from the
     * bad_publishers view.
     */
    internal fun createBadIndiciaPublishersView() {
        val query =
            """CREATE OR REPLACE VIEW $targetSchema.bad_indicia_publishers AS (
                SELECT gip.id
                FROM gcd_indicia_publisher gip
                WHERE gip.parent_id IN (SELECT id FROM bad_publishers));""".trimIndent()

        queryExecutor.executeSqlStatement(query)
    }

    /**
     * Create the bad_brand_groups view.
     *
     * This view is used to filter out brand groups that are from the
     * bad_publishers view.
     */
    internal fun createBadBrandGroupsView() {
        val query =
            """CREATE OR REPLACE VIEW $targetSchema.bad_brand_groups AS (
                SELECT gbg.id
                FROM gcd_brand_group gbg
                WHERE gbg.parent_id IN (SELECT id FROM bad_publishers));""".trimIndent()

        queryExecutor.executeSqlStatement(query)
    }
}