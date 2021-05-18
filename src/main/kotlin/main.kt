import com.mysql.cj.jdbc.Driver
import java.sql.*
import java.util.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

private const val USERNAME = "wbenica"
private const val PASSWORD = "773Mysql!"

var i = 0

fun main(args: Array<String>) = runBlocking {
    val updater = Updater()

    updater.update()
}


class Updater {
    private var conn: Connection? = null

    suspend fun update() {
        try {
            getConnection()
            val updateDatabase = coroutineScope {
                async {
//                    addTables()
//                    shrinkDatabase()
                    delay(10)
                }
            }
            updateDatabase.await().let {
                println("Starting Credits...")
                updateCredits()
            }
        } finally {
            closeConn()
        }
    }

    private fun getConnection() {
        val connectionProps = Properties()
        connectionProps["user"] = USERNAME
        connectionProps["password"] = PASSWORD

        try {
            Driver()
            conn = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/gcdb2",
                connectionProps
            )
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun addTables() {
        val stmt = conn?.createStatement()
        try {
            val fr = FileReader(File("./src/main/kotlin/my_tables.sql"))
            val br = BufferedReader(fr)
            val sb = StringBuffer()
            var s: String?
            while (br.readLine().also { s = it } != null) {
                sb.append(s)
            }
            br.close()
            val instructions = sb.toString().split(';')
            instructions.forEach {
                println(it)
                println()
                stmt?.executeUpdate(it)
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {

        }
    }

    private fun shrinkDatabase() {
        val stmt = conn?.createStatement()
        try {
            val fr = FileReader(File("./src/main/kotlin/remove_records.sql"))
            val br = BufferedReader(fr)
            val sb = StringBuffer()
            var s: String?
            while (br.readLine().also { s = it } != null) {
                sb.append(s)
            }
            br.close()
            val instructions = sb.toString().split(';')
            instructions.forEach {
                if (it != "") {
                    println("This is $it")
                    println()
                    stmt?.executeUpdate(it)
                }
            }
        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun updateCredits() {
        val scriptStmt: Statement?
        var scriptResultSet: ResultSet? = null

        try {
            scriptStmt = conn?.createStatement()
            val scriptSql = """SELECT g.script, g.id, g.pencils, g.inks, g.colors, g.letters, g.editing
                FROM gcd_story g;"""

            scriptResultSet = scriptStmt?.executeQuery(scriptSql)

            if (scriptStmt?.execute(scriptSql) == true) {
                scriptResultSet = scriptStmt.resultSet
            }

            while (scriptResultSet?.next() == true) {
                val scriptNames = scriptResultSet.getString("script").split(';')
                val pencilsNames = scriptResultSet.getString("pencils").split(';')
                val inksNames = scriptResultSet.getString("inks").split(';')
                val colorsNames = scriptResultSet.getString("colors").split(';')
                val lettersNames = scriptResultSet.getString("letters").split(';')
                val editingNames = scriptResultSet.getString("editing").split(';')

                val storyId = scriptResultSet.getInt("id")

                makeCredits(scriptNames, storyId, 1)
                makeCredits(pencilsNames, storyId, 2)
                makeCredits(inksNames, storyId, 3)
                makeCredits(colorsNames, storyId, 4)
                makeCredits(lettersNames, storyId, 5)
                makeCredits(editingNames, storyId, 6)
            }
            println("END")
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            closeResultSet(scriptResultSet)
        }
    }

    private fun closeConn() {
        if (conn != null) {
            try {
                conn?.close()
            } catch (sqlEx: SQLException) {
                sqlEx.printStackTrace()
            }
            conn = null
        }
    }

    private fun closeResultSet(resultSet: ResultSet?) {
        if (resultSet != null) {
            try {
                resultSet.close()
            } catch (sqlEx: SQLException) {
                sqlEx.printStackTrace()
            }
        }
    }

    private suspend fun makeCredits(script_names: List<String>, storyId: Int, roleId: Int) {
        for (name in script_names) {
            if (name != "") {
                coroutineScope {
                    launch {
                        makeCredit(prepareName(name), storyId, roleId)
                    }
                }
            }
        }
    }

    private fun prepareName(name: String): String {
        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
        res = res.replace(Regex("\\s*\\?\\s*"), "")
        res = res.replace(Regex("^\\s*"), "")
        return res
    }

    private fun getGcsc(gcndId: Int, storyId: Int, roleId: Int): Int? {
        var storyCreditId: Int? = null
        val getGcscStmnt: PreparedStatement?
        var gcscResultSet: ResultSet? = null

        try {
            val getGcSql = """
                    SELECT gsc.id
                    FROM gcd_story_credit gsc
                    WHERE gsc.creator_id = ?
                    AND gsc.story_id = ?
                    AND gsc.credit_type_id = ?
            """

            getGcscStmnt = conn?.prepareStatement(getGcSql)
            getGcscStmnt?.setInt(1, gcndId)
            getGcscStmnt?.setInt(2, storyId)
            getGcscStmnt?.setInt(3, roleId)

            gcscResultSet = getGcscStmnt?.executeQuery()

            if (getGcscStmnt?.execute() == true) {
                gcscResultSet = getGcscStmnt.resultSet
            }

            if (gcscResultSet?.next() == true) {
                storyCreditId = gcscResultSet.getInt("id")
            }

        } catch (sqlEx: SQLException) {
            sqlEx.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            closeResultSet(gcscResultSet)
        }

        return storyCreditId
    }

    private fun getGcnd(extracted_name: String): Int? {
        var gcndId: Int? = null
        val getGcndStmnt: PreparedStatement?
        var gcndResultSet: ResultSet? = null

        try {
            val getGcndSql = """
               SELECT * 
               FROM gcd_creator_name_detail gcnd
               WHERE gcnd.name = ?                
            """

            getGcndStmnt = conn?.prepareStatement(getGcndSql)
            getGcndStmnt?.setString(1, extracted_name)

            gcndResultSet = getGcndStmnt?.executeQuery()

            if (getGcndStmnt?.execute() == true) {
                gcndResultSet = getGcndStmnt.resultSet
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

    private fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {


        val gcndId = getGcnd(extracted_name)

        if (gcndId != null) {

            val gcscId = getGcsc(gcndId, storyId, roleId)

            if (gcscId == null) {

                val insertStoryCreditStmt: PreparedStatement?

                val insertStoryCreditSql = """
                        INSERT INTO m_story_credit(created, modified, deleted, is_credited, is_signed, uncertain, signed_as, credited_as, credit_name, creator_id, credit_type_id, story_id, signature_id)
                         VALUE (CURTIME(), curtime(), 0, 0, 0, 0, '', '', '', ?, ?, ?, NULL)
                    """

                insertStoryCreditStmt = conn?.prepareStatement(insertStoryCreditSql)
                insertStoryCreditStmt?.setInt(1, gcndId)
                insertStoryCreditStmt?.setInt(2, roleId)
                insertStoryCreditStmt?.setInt(3, storyId)

                insertStoryCreditStmt?.executeUpdate()
            }
        }
    }
}