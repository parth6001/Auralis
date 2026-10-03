package com.auralis.player

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.auralis.player.data.LibraryDatabase
import org.junit.Assert.*
import org.json.JSONObject
import org.junit.Test

class MigrationTest {
    @Test fun versionOneRetainsSettingsAndAddsMood() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-proof"
        context.deleteDatabase(name)
        // Read the exported schema directly: the test-helper serializer is incompatible
        // with the selected Kotlin runtime. Room still validates the migrated schema.
        val schema = instrumentation.context.assets.open("com.auralis.player.data.LibraryDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                fun sql(value: String) = value.replace("\${TABLE_NAME}", table)
                old.execSQL(sql(entity.getString("createSql")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(sql(indices.getJSONObject(j).getString("createSql")))
                if (table == "Song") {
                    val fields = entity.getJSONArray("fields")
                    val values = android.content.ContentValues()
                    for (j in 0 until fields.length()) {
                        val field = fields.getJSONObject(j)
                        val column = field.getString("columnName")
                        if (field.getString("affinity") == "TEXT") values.put(column, "migration-$column") else values.put(column, 1L)
                    }
                    assertTrue(old.insertOrThrow(table, null, values) != -1L)
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO Setting (`key`, value) VALUES ('theme', 'AMOLED')")
            old.version = 1
        }
        val database = Room.databaseBuilder(context, LibraryDatabase::class.java, name)
            .addMigrations(LibraryDatabase.MIGRATION_1_2).build()
        try {
                val db = database.openHelper.writableDatabase
                assertEquals(2, db.version)
                db.query("SELECT value FROM Setting WHERE `key` = 'theme'").use {
                    assertTrue(it.moveToFirst()); assertEquals("AMOLED", it.getString(0))
                }
                db.query("SELECT title, favourite, mood FROM Song WHERE id = 'migration-id'").use {
                    assertTrue(it.moveToFirst())
                    assertEquals("migration-title", it.getString(0))
                    assertEquals(1, it.getInt(1))
                    assertEquals("", it.getString(2))
                }
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
