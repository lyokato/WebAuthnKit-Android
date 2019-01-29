package webauthnkit.core.authenticator.internal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteOpenHelper
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil
import java.lang.Exception

@ExperimentalUnsignedTypes
class CredentialStore(context: Context) {

    companion object {
        val TAG = CredentialStore::class.simpleName
        private const val DatabaseName = "webauthnkit.db"
        private const val DatabaseVersion = 1
    }

    private val db = CredentialStoreDatabaseHelper(
        context = context,
        name    = DatabaseName,
        version = DatabaseVersion
    )

    fun loadAllCredentialSources(rpId: String): List<PublicKeyCredentialSource> {
        WAKLogger.d(TAG, "loadAllCredentialSource")
        return db.searchByRpId(rpId)
    }

    fun loadAllCredentialSources(rpId: String, userHandle: ByteArray): List<PublicKeyCredentialSource> {
        WAKLogger.d(TAG, "loadAllCredentialSource")
        return loadAllCredentialSources(rpId).filter {
            ByteArrayUtil.equals(it.userHandle, userHandle)
        }
    }

    fun deleteAllCredentialSources(rpId: String) {
        WAKLogger.d(TAG, "deleteAllCredentialSource")
        db.deleteByRpId(rpId)
    }

    fun deleteAllCredentialSources(rpId: String, userHandle: ByteArray) {
        WAKLogger.d(TAG, "deleteAllCredentialSource")
        loadAllCredentialSources(rpId, userHandle).forEach {
            val key = ByteArrayUtil.toHex(it.id)
            db.delete(key)
        }
    }

    fun lookupCredentialSource(credentialId: ByteArray): PublicKeyCredentialSource? {
        WAKLogger.d(TAG, "lookupCredentialSource")
        val key = ByteArrayUtil.toHex(credentialId)
        return db.findById(key)
    }

    fun saveCredentialSource(source: PublicKeyCredentialSource): Boolean {
        WAKLogger.d(TAG, "saveCredentialSource")

        val content = source.toBase64()
        return if (content != null) {
            db.save(
                id      = source.idHex,
                rpId    = source.rpId,
                content = content
            )
        } else {
            WAKLogger.d(TAG, "failed to encode content")
            false
        }
    }
}

@ExperimentalUnsignedTypes
class CredentialStoreDatabaseHelper(
    context: Context,
    name:    String,
    version: Int
): SQLiteOpenHelper(context, name, null, version) {

    companion object {
        private val TAG = CredentialStoreDatabaseHelper::class.simpleName
        private const val TableName = "credentials"
        private const val ColumnId       = "id"
        private const val ColumnRpId     = "rp_id"
        private const val ColumnContent  = "content"
        private const val ColumnOnUpdate = "on_update"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val tableSQL = """
            CREATE TABLE $TableName(
                $ColumnId       TEXT PRIMARY KEY,
                $ColumnRpId     TEXT NOT NULL,
                $ColumnContent  TEXT NOT NULL,
                $ColumnOnUpdate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
            );
        """.trimMargin()

        val indexSQL = """
            CREATE INDEX credentials_index
            ON credentials ($ColumnRpId);
        """.trimIndent()

        db.execSQL(tableSQL)
        db.execSQL(indexSQL)
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVer: Int, currentVer: Int) {
        WAKLogger.w(TAG, "onUpgrade")
    }

    fun findById(id: String): PublicKeyCredentialSource? {
        WAKLogger.w(TAG, "findById")
        val db = readableDatabase
        val cursor = db.query(TableName,
            arrayOf(ColumnId, ColumnRpId, ColumnContent),
            "$ColumnId = ?",
            arrayOf(id),
            null,
            null,
            "$ColumnOnUpdate DESC"
        )
        cursor.use {
            return if (it.moveToNext()) {
                val content = it.getString(it.getColumnIndex(ColumnContent))
                PublicKeyCredentialSource.fromBase64(content)
            } else {
                WAKLogger.w(TAG, "not found")
                null
            }
        }
    }

    fun delete(id: String):Boolean {
        WAKLogger.d(TAG, "delete")
        val db = writableDatabase
        return try {
            db.delete(TableName, "$ColumnId = ?", arrayOf(id))
            true
        } catch (e: Exception) {
            WAKLogger.w(TAG, "failed to delete: " + e.localizedMessage)
            false
        }
    }

    fun deleteByRpId(rpId: String): Boolean {
        WAKLogger.d(TAG, "deleteByRpId")
        val db = writableDatabase

        return try {
            db.delete(TableName, "$ColumnRpId = ?", arrayOf(rpId))
            true
        } catch (e: Exception) {
            WAKLogger.w(TAG, "failed to delete: " + e.localizedMessage)
            false
        }
    }

    fun deleteAll(): Boolean {
        WAKLogger.d(TAG, "deleteAll")
        val db = writableDatabase

        return try {
            db.delete(TableName, null, null)
            true
        } catch (e: Exception) {
            WAKLogger.w(TAG, "failed to delete: " + e.localizedMessage)
            false
        }
    }

    fun searchByRpId(rpId: String): List<PublicKeyCredentialSource> {
        WAKLogger.d(TAG, "searchByRpId: $rpId")

        val db = readableDatabase

        val cursor = db.query(TableName,
            arrayOf(ColumnId, ColumnRpId, ColumnContent),
            "$ColumnRpId = ?",
            arrayOf(rpId),
            null,
            null,
            "$ColumnOnUpdate DESC"
        )

        cursor.use {

            WAKLogger.d(TAG, "searchByRpId: cursor")

            val results: MutableList<PublicKeyCredentialSource> = mutableListOf()

            while (it.moveToNext()) {
                WAKLogger.d(TAG, "searchByRpId: iterate")
                val content = it.getString(it.getColumnIndex(ColumnContent))
                val source = PublicKeyCredentialSource.fromBase64(content)
                if (source != null) {
                    results.add(source)
                } else {
                    WAKLogger.w(TAG, "invalid format of credential-source")
                    // XXX should delete this record?
                }
            }

            return results
        }
    }

    fun save(id: String, rpId: String, content: String): Boolean {
        WAKLogger.d(TAG, "save $rpId")

        val db = writableDatabase

        return try {
            val values = ContentValues()
            values.put(ColumnId, id)
            values.put(ColumnRpId, rpId)
            values.put(ColumnContent, content)
            db.insertWithOnConflict(TableName, null, values, CONFLICT_REPLACE)
            true
        } catch (e: Exception) {
            WAKLogger.w(TAG, "failed to insert: " + e.localizedMessage)
            false
        }
    }
}

