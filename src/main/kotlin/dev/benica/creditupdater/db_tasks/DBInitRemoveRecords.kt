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
    
    fun deleteGcdReprintFromBadStories() {
        val query = """DELETE gr
            FROM $targetSchema.gcd_reprint gr
            WHERE gr.origin_id IN (SELECT id FROM $targetSchema.bad_stories)
            OR gr.target_id IN (SELECT id FROM $targetSchema.bad_stories);
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
    
    fun deleteGcdIssueFromBadSeries() {
        val query = """DELETE gi
            FROM $targetSchema.gcd_issue gi
            WHERE gi.series_id IN (SELECT id FROM $targetSchema.bad_series);
        """.trimIndent()
        
        queryExecutor.executeSqlStatement(query, conn)
    }
}