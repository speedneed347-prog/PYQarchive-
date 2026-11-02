package com.example.data.remote

import android.os.Build
import android.util.Log
import com.example.data.local.CourseEntity
import com.example.data.local.ExamScheduleEntity
import com.example.data.local.LanguageEntity
import com.example.data.local.PaperEntity
import com.example.data.local.PaperTypeEntity
import com.example.data.local.SemesterEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UniversityEntity
import com.example.data.local.YearEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FirestoreRestClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val projectId: String = "pyqhub-new",
    private val apiKey: String = "AIzaSyB3hxWdH9ezqnffoArRzzt0-1xt1FmVaEE"
) {

    private val baseUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    private fun buildUrl(collection: String, pageSize: Int = 300, pageToken: String? = null): String {
        val sb = StringBuilder("$baseUrl/$collection?pageSize=$pageSize&key=$apiKey")
        if (!pageToken.isNullOrBlank()) {
            sb.append("&pageToken=$pageToken")
        }
        return sb.toString()
    }

    private suspend fun fetchCollectionRaw(collection: String, pageSize: Int = 300): List<JSONObject> = withContext(Dispatchers.IO) {
        val documents = mutableListOf<JSONObject>()
        var pageToken: String? = null

        for (page in 1..5) {
            val url = buildUrl(collection, pageSize, pageToken)
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Failed to fetch $collection: HTTP ${response.code}")
                        return@use
                    }
                    val bodyString = response.body?.string() ?: return@use
                    val json = JSONObject(bodyString)
                    val docsArray = json.optJSONArray("documents")
                    if (docsArray != null) {
                        for (i in 0 until docsArray.length()) {
                            docsArray.optJSONObject(i)?.let { documents.add(it) }
                        }
                    }
                    pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Error fetching collection $collection: ${t.message}")
                break
            }

            if (pageToken == null) break
        }
        documents
    }

    suspend fun fetchUniversities(): List<UniversityEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("universities").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            UniversityEntity(
                id = id,
                name = getString(fields, "name") ?: id,
                shortName = getString(fields, "short_name") ?: getString(fields, "shortName") ?: "",
                description = getString(fields, "description") ?: "",
                logo = getString(fields, "logo") ?: ""
            )
        }
    }

    suspend fun fetchCourses(): List<CourseEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("courses").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            CourseEntity(
                id = id,
                name = getString(fields, "name") ?: id
            )
        }
    }

    suspend fun fetchSemesters(): List<SemesterEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("semesters").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            SemesterEntity(
                id = id,
                name = getString(fields, "name") ?: id
            )
        }
    }

    suspend fun fetchSubjects(): List<SubjectEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("subjects").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            SubjectEntity(
                id = id,
                name = getString(fields, "name") ?: id,
                code = getString(fields, "code") ?: ""
            )
        }
    }

    suspend fun fetchYears(): List<YearEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("years").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            val yearVal = getString(fields, "year") ?: getString(fields, "name") ?: id
            YearEntity(id = id, year = yearVal)
        }
    }

    suspend fun fetchPaperTypes(): List<PaperTypeEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("paper_types").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            PaperTypeEntity(
                id = id,
                name = getString(fields, "name") ?: id
            )
        }
    }

    suspend fun fetchLanguages(): List<LanguageEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("languages").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            LanguageEntity(
                id = id,
                name = getString(fields, "name") ?: id
            )
        }
    }

    suspend fun fetchExamSchedules(
        uniMap: Map<String, String> = emptyMap(),
        semMap: Map<String, String> = emptyMap()
    ): List<ExamScheduleEntity> = withContext(Dispatchers.IO) {
        fetchCollectionRaw("exam_schedules").mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
            val uId = getString(fields, "universityId") ?: ""
            val sId = getString(fields, "semesterId") ?: ""
            ExamScheduleEntity(
                id = id,
                universityId = uId,
                universityName = uniMap[uId] ?: getString(fields, "universityName") ?: uId,
                semesterId = sId,
                semesterName = semMap[sId] ?: getString(fields, "semesterName") ?: sId,
                startDateMillis = parseIsoTimestamp(getTimestamp(fields, "startDate")),
                endDateMillis = parseIsoTimestamp(getTimestamp(fields, "endDate")),
                academicYear = getString(fields, "academicYear") ?: ""
            )
        }
    }

    suspend fun fetchPapers(): List<PaperEntity> = withContext(Dispatchers.IO) {
        val docs = fetchCollectionRaw("papers", pageSize = 300)
        docs.mapNotNull { doc ->
            val id = doc.optString("name").substringAfterLast("/")
            val fields = doc.optJSONObject("fields") ?: return@mapNotNull null

            val title = getString(fields, "title") ?: "Untitled Paper"
            val pdfUrl = getString(fields, "pdfUrl") ?: ""
            val status = getString(fields, "status") ?: "published"

            // Ignore drafts or unpublished if status explicitly set to non-published
            if (status.isNotBlank() && status != "published") {
                return@mapNotNull null
            }

            PaperEntity(
                id = id,
                title = title,
                pdfUrl = pdfUrl,
                universityId = getString(fields, "universityId") ?: "",
                universityName = getString(fields, "universityName") ?: "",
                courseId = getString(fields, "courseId") ?: "",
                courseName = getString(fields, "courseName") ?: "",
                semesterId = getString(fields, "semesterId") ?: "",
                semesterName = getString(fields, "semesterName") ?: "",
                subjectId = getString(fields, "subjectId") ?: "",
                subjectName = getString(fields, "subjectName") ?: "",
                yearId = getString(fields, "yearId") ?: "",
                year = getString(fields, "year") ?: "",
                paperTypeId = getString(fields, "paperTypeId") ?: "",
                paperTypeName = getString(fields, "paperTypeName") ?: "",
                languageId = getString(fields, "languageId") ?: "",
                languageName = getString(fields, "languageName") ?: "",
                status = status,
                description = getString(fields, "description") ?: "",
                viewCount = getLong(fields, "viewCount"),
                createdAtMillis = parseIsoTimestamp(getTimestamp(fields, "createdAt"))
            )
        }
    }

    suspend fun fetchPaperById(paperId: String): PaperEntity? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/papers/$paperId?key=$apiKey"
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val doc = JSONObject(body)
                val id = doc.optString("name").substringAfterLast("/")
                val fields = doc.optJSONObject("fields") ?: return@use null
                PaperEntity(
                    id = id,
                    title = getString(fields, "title") ?: "Untitled Paper",
                    pdfUrl = getString(fields, "pdfUrl") ?: "",
                    universityId = getString(fields, "universityId") ?: "",
                    universityName = getString(fields, "universityName") ?: "",
                    courseId = getString(fields, "courseId") ?: "",
                    courseName = getString(fields, "courseName") ?: "",
                    semesterId = getString(fields, "semesterId") ?: "",
                    semesterName = getString(fields, "semesterName") ?: "",
                    subjectId = getString(fields, "subjectId") ?: "",
                    subjectName = getString(fields, "subjectName") ?: "",
                    yearId = getString(fields, "yearId") ?: "",
                    year = getString(fields, "year") ?: "",
                    paperTypeId = getString(fields, "paperTypeId") ?: "",
                    paperTypeName = getString(fields, "paperTypeName") ?: "",
                    languageId = getString(fields, "languageId") ?: "",
                    languageName = getString(fields, "languageName") ?: "",
                    status = getString(fields, "status") ?: "published",
                    description = getString(fields, "description") ?: "",
                    viewCount = getLong(fields, "viewCount"),
                    createdAtMillis = parseIsoTimestamp(getTimestamp(fields, "createdAt"))
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error fetching paper by ID $paperId: ${t.message}")
            null
        }
    }

    private fun getString(fields: JSONObject, key: String): String? {
        val obj = fields.optJSONObject(key) ?: return null
        return when {
            obj.has("stringValue") -> obj.optString("stringValue")
            obj.has("integerValue") -> obj.optString("integerValue")
            obj.has("doubleValue") -> obj.optString("doubleValue")
            else -> null
        }
    }

    private fun getLong(fields: JSONObject, key: String): Long {
        val obj = fields.optJSONObject(key) ?: return 0L
        return when {
            obj.has("integerValue") -> obj.optString("integerValue").toLongOrNull() ?: 0L
            obj.has("doubleValue") -> obj.optDouble("doubleValue", 0.0).toLong()
            else -> 0L
        }
    }

    private fun getTimestamp(fields: JSONObject, key: String): String? {
        val obj = fields.optJSONObject(key) ?: return null
        return obj.optString("timestampValue").takeIf { it.isNotBlank() }
    }

    private fun parseIsoTimestamp(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.Instant.parse(isoString).toEpochMilli()
            } else {
                val clean = isoString.substringBefore(".").substringBefore("Z")
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(clean)?.time
            }
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        private const val TAG = "FirestoreRestClient"
    }
}
