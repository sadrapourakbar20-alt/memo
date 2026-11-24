package com.sadra.memo.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.sadra.memo.data.database.AppDatabase
import com.sadra.memo.data.entities.CancellationEntity
import com.sadra.memo.data.entities.ClassEntity
import com.sadra.memo.data.entities.NoteEntity
import com.sadra.memo.data.entities.TaskEntity
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.domain.model.SemesterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate

class BackupManager(
    private val db: AppDatabase,
    private val settings: SettingsRepository
) {
    suspend fun export(resolver: ContentResolver, uri: Uri) = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Memo")
        root.put("version", 1)

        val notes = JSONArray()
        db.noteDao().getAll().forEach { n ->
            notes.put(
                JSONObject()
                    .put("id", n.id).put("title", n.title).put("content", n.content)
                    .put("category", n.category).put("isPinned", n.isPinned)
                    .put("createdAt", n.createdAt).put("updatedAt", n.updatedAt)
                    .put("isDeleted", n.isDeleted).put("isArchived", n.isArchived)
            )
        }
        root.put("notes", notes)

        val tasks = JSONArray()
        db.taskDao().getAll().forEach { t ->
            tasks.put(
                JSONObject()
                    .put("id", t.id).put("title", t.title).put("description", t.description)
                    .put("dueAt", t.dueAt ?: JSONObject.NULL)
                    .put("priority", t.priority).put("progress", t.progress)
                    .put("isDone", t.isDone).put("createdAt", t.createdAt)
                    .put("repeatType", t.repeatType).put("alertOffsets", t.alertOffsets)
                    .put("subtasks", t.subtasks)
            )
        }
        root.put("tasks", tasks)

        val classes = JSONArray()
        db.classDao().getAll().forEach { c ->
            classes.put(
                JSONObject()
                    .put("id", c.id).put("name", c.name).put("teacher", c.teacher)
                    .put("location", c.location).put("weekday", c.weekday)
                    .put("startMinutes", c.startMinutes).put("endMinutes", c.endMinutes)
                    .put("pattern", c.pattern).put("alertOffsets", c.alertOffsets)
            )
        }
        root.put("classes", classes)

        val cancellations = JSONArray()
        db.classDao().getAllCancellations().forEach { c ->
            cancellations.put(
                JSONObject().put("id", c.id).put("classId", c.classId).put("epochDay", c.epochDay)
            )
        }
        root.put("cancellations", cancellations)

        val sem = settings.semesterConfig.first()
        root.put(
            "semester",
            JSONObject()
                .put("start", sem.start?.toEpochDay() ?: JSONObject.NULL)
                .put("end", sem.end?.toEpochDay() ?: JSONObject.NULL)
                .put("invert", sem.invertParity)
        )

        val out = resolver.openOutputStream(uri) ?: throw IOException("فایل باز نشد")
        out.bufferedWriter(Charsets.UTF_8).use { it.write(root.toString(2)) }
    }

    suspend fun import(resolver: ContentResolver, uri: Uri) = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(uri) ?: throw IOException("فایل باز نشد")
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        if (root.optString("app") != "Memo") throw IOException("این فایل پشتیبان Memo نیست")

        val notes = ArrayList<NoteEntity>()
        val na = root.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until na.length()) {
            val o = na.getJSONObject(i)
            notes.add(
                NoteEntity(
                    id = o.getLong("id"), title = o.getString("title"), content = o.getString("content"),
                    category = o.getString("category"), isPinned = o.getBoolean("isPinned"),
                    createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
                    isDeleted = o.optBoolean("isDeleted", false), isArchived = o.optBoolean("isArchived", false)
                )
            )
        }

        val tasks = ArrayList<TaskEntity>()
        val ta = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until ta.length()) {
            val o = ta.getJSONObject(i)
            tasks.add(
                TaskEntity(
                    id = o.getLong("id"), title = o.getString("title"), description = o.getString("description"),
                    dueAt = if (o.isNull("dueAt")) null else o.getLong("dueAt"),
                    priority = o.getInt("priority"), progress = o.getInt("progress"),
                    isDone = o.getBoolean("isDone"), createdAt = o.getLong("createdAt"),
                    repeatType = o.optInt("repeatType", 0), alertOffsets = o.optString("alertOffsets", ""),
                    subtasks = o.optString("subtasks", "")
                )
            )
        }

        val classes = ArrayList<ClassEntity>()
        val ca = root.optJSONArray("classes") ?: JSONArray()
        for (i in 0 until ca.length()) {
            val o = ca.getJSONObject(i)
            classes.add(
                ClassEntity(
                    id = o.getLong("id"), name = o.getString("name"), teacher = o.getString("teacher"),
                    location = o.getString("location"), weekday = o.getInt("weekday"),
                    startMinutes = o.getInt("startMinutes"), endMinutes = o.getInt("endMinutes"),
                    pattern = o.getInt("pattern"), alertOffsets = o.getString("alertOffsets")
                )
            )
        }

        val cancellations = ArrayList<CancellationEntity>()
        val xa = root.optJSONArray("cancellations") ?: JSONArray()
        for (i in 0 until xa.length()) {
            val o = xa.getJSONObject(i)
            cancellations.add(
                CancellationEntity(id = o.getLong("id"), classId = o.getLong("classId"), epochDay = o.getLong("epochDay"))
            )
        }

        var semester: SemesterConfig? = null
        val so = root.optJSONObject("semester")
        if (so != null) {
            semester = SemesterConfig(
                start = if (so.isNull("start")) null else LocalDate.ofEpochDay(so.getLong("start")),
                end = if (so.isNull("end")) null else LocalDate.ofEpochDay(so.getLong("end")),
                invertParity = so.optBoolean("invert", false)
            )
        }

        // همه‌چیز با موفقیت خونده شد؛ حالا جایگزین می‌کنیم
        db.withTransaction {
            db.noteDao().clearAll()
            db.taskDao().clearAll()
            db.classDao().clearClasses()
            db.classDao().clearCancellations()
            notes.forEach { db.noteDao().upsert(it) }
            tasks.forEach { db.taskDao().upsert(it) }
            classes.forEach { db.classDao().upsert(it) }
            cancellations.forEach { db.classDao().upsertCancellation(it) }
        }
        if (semester != null) settings.setSemester(semester)
    }
}
