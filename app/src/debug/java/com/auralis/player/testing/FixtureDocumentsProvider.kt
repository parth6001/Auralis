package com.auralis.player.testing

import android.database.Cursor
import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import java.io.File

/** Private debug-only document provider exercises the real SAF scanner with generated fixtures. */
class FixtureDocumentsProvider : ContentProvider() {
    override fun onCreate() = true
    private fun file(id: String): File {
        val root = File(requireNotNull(context).filesDir, "scanner-fixtures").canonicalFile
        val file = if (id == "root") root else File(root, id).canonicalFile
        require(file == root || file.path.startsWith(root.path + File.separator))
        return file
    }
    private val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS)
    private fun row(cursor: MatrixCursor, id: String) {
        val f = file(id)
        val values: Map<String, Any> = mapOf(Document.COLUMN_DOCUMENT_ID to id, Document.COLUMN_DISPLAY_NAME to f.name,
            Document.COLUMN_MIME_TYPE to if (f.isDirectory) Document.MIME_TYPE_DIR else when (f.extension) { "wav" -> "audio/wav"; "mp3" -> "audio/mpeg"; "flac" -> "audio/flac"; else -> "audio/unknown" },
            Document.COLUMN_SIZE to f.length(), Document.COLUMN_LAST_MODIFIED to f.lastModified(), Document.COLUMN_FLAGS to 0)
        cursor.addRow(cursor.columnNames.map { values[it] }.toTypedArray())
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val segments = uri.pathSegments
        return if (segments.last() == "children") queryChildren(segments[segments.size - 2], projection)
        else MatrixCursor(projection ?: columns).apply { row(this, segments.last()) }
    }
    private fun queryChildren(parentDocumentId: String, projection: Array<out String>?): Cursor {
        check(!File(file("root"), ".fail").exists()) { "Simulated provider failure" }
        return MatrixCursor(projection ?: columns).apply {
            file(parentDocumentId).listFiles()?.filterNot { it.name.startsWith('.') }?.forEach { f -> row(this, f.relativeTo(file("root")).invariantSeparatorsPath) }
        }
    }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r")
        return ParcelFileDescriptor.open(file(requireNotNull(uri.lastPathSegment)), ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun getType(uri: Uri): String = "audio/wav"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("Read only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("Read only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("Read only")
}
