import DatabaseUtil.Companion.closeResultSet
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class CreditExtractor(database: String, conn: Connection) : Extractor(database, conn) {
    override suspend fun extract(
        extractFrom: ResultSet,
        destDatabase: String?
    ): Int {
        val storyId = extractFrom.getInt("id")

        val scriptNames = extractFrom.getString("script").split(';')
        val pencilsNames = extractFrom.getString("pencils").split(';')
        val inksNames = extractFrom.getString("inks").split(';')
        val colorsNames = extractFrom.getString("colors").split(';')
        val lettersNames = extractFrom.getString("letters").split(';')
        val editingNames = extractFrom.getString("editing").split(';')

        makeCredits(scriptNames, storyId, 1)
        makeCredits(pencilsNames, storyId, 2)
        makeCredits(inksNames, storyId, 3)
        makeCredits(colorsNames, storyId, 4)
        makeCredits(lettersNames, storyId, 5)
        makeCredits(editingNames, storyId, 6)
        return storyId
    }

    override fun finish() {
        // do nothing
    }

    private fun makeCredits(
        script_names: List<String>,
        storyId: Int,
        roleId: Int
    ) {
        for (name in script_names) {
            if (name != "") {
                makeCredit(name.prepareName(), storyId, roleId)
            }
        }
    }

    private fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {
        getGcnd(extracted_name, conn)?.let { gcndId ->
            getStoryCredit(gcndId, storyId, roleId) ?: makeStoryCredit(
                gcndId,
                roleId,
                storyId
            )
        }
    }

    private fun getGcnd(extracted_name: String, conn: Connection?): Int? {
        var gcndId: Int? = null
        val getCondStmt: PreparedStatement?
        var gcndResultSet: ResultSet? = null

        try {
            val getGcndSql = """
               SELECT * 
               FROM gcd_creator_name_detail gcnd
               WHERE gcnd.name = ?                
            """

            getCondStmt = conn?.prepareStatement(getGcndSql)
            getCondStmt?.setString(1, extracted_name)

            gcndResultSet = getCondStmt?.executeQuery()

            if (getCondStmt?.execute() == true) {
                gcndResultSet = getCondStmt.resultSet
            }

            if (gcndResultSet?.next() == true) {
                gcndId = gcndResultSet.getInt("id")
            } else {
                // TODO: save extracted_name storyId, Role to file
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(gcndResultSet)
        }

        return gcndId
    }

    private fun getStoryCredit(gcndId: Int, storyId: Int, roleId: Int): Int? {
        var storyCreditId: Int? = null
        val statement: PreparedStatement?
        var resultSet: ResultSet? = null

        try {
            val getStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${database}.gcd_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

            statement = conn.prepareStatement(getStoryCreditIdSql)
            statement?.setInt(1, gcndId)
            statement?.setInt(2, storyId)
            statement?.setInt(3, roleId)

            resultSet = statement?.executeQuery()

            if (statement?.execute() == true) {
                resultSet = statement.resultSet
            }

            if (resultSet?.next() == true) {
                storyCreditId = resultSet.getInt("id")
            }

        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(resultSet)
        }

        if (storyCreditId == null) {
            val statement2: PreparedStatement?
            try {
                val getMStoryCreditIdSql = """
                    SELECT gsc.id
                    FROM ${database}.m_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

                statement2 = conn.prepareStatement(getMStoryCreditIdSql)
                statement2?.setInt(1, gcndId)
                statement2?.setInt(2, storyId)
                statement2?.setInt(3, roleId)

                resultSet = statement2?.executeQuery()

                if (statement2?.execute() == true) {
                    resultSet = statement2.resultSet
                }

                if (resultSet?.next() == true) {
                    storyCreditId = resultSet.getInt("id")
                }
            } catch (sqlEx: SQLException) {
                sqlEx.printStackTrace()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                closeResultSet(resultSet)
            }
        }

        return storyCreditId
    }

    private fun makeStoryCredit(gcndId: Int, roleId: Int, storyId: Int) {
        val insertStoryCreditStmt: PreparedStatement?

        val insertStoryCreditSql = """
                            INSERT INTO $database.m_story_credit(created, modified, deleted, is_credited, is_signed, uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, signature_id)
                             VALUE (CURTIME(), CURTIME(), 0, 0, 0, 0, '', '', '', ?, ?, ?, NULL)
                        """

        insertStoryCreditStmt = conn.prepareStatement(insertStoryCreditSql)
        insertStoryCreditStmt?.setInt(1, gcndId)
        insertStoryCreditStmt?.setInt(2, roleId)
        insertStoryCreditStmt?.setInt(3, storyId)

        insertStoryCreditStmt?.executeUpdate()
    }
}