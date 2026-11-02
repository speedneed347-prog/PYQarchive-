package com.example.data.repository

import android.content.Context
import com.example.data.local.CourseEntity
import com.example.data.local.ExamScheduleEntity
import com.example.data.local.LanguageEntity
import com.example.data.local.PaperEntity
import com.example.data.local.PaperTypeEntity
import com.example.data.local.PyqDatabase
import com.example.data.local.SemesterEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UniversityEntity
import com.example.data.local.YearEntity
import com.example.data.model.Course
import com.example.data.model.ExamSchedule
import com.example.data.model.LanguageItem
import com.example.data.model.Paper
import com.example.data.model.PaperType
import com.example.data.model.Semester
import com.example.data.model.Subject
import com.example.data.model.University
import com.example.data.model.YearItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ArchiveRepository(private val context: Context) {

    companion object {
        @Volatile
        private var settingsConfigured = false
    }

    private val firestore: FirebaseFirestore by lazy {
        val instance = FirebaseFirestore.getInstance()
        if (!settingsConfigured) {
            synchronized(ArchiveRepository::class.java) {
                if (!settingsConfigured) {
                    try {
                        val settings = FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .build()
                        instance.firestoreSettings = settings
                    } catch (_: Throwable) {
                        // Settings can only be configured once before any Firestore operation.
                        // Persistence is on by default in Android Firestore SDK.
                    }
                    settingsConfigured = true
                }
            }
        }
        instance
    }

    private val db = PyqDatabase.getInstance(context)
    private val paperDao = db.paperDao()
    private val refDao = db.referenceDao()

    // ----------------------------------------------------
    // PAPERS FLOWS (ROOM BACKED FOR SEAMLESS OFFLINE)
    // ----------------------------------------------------

    val popularPapers: Flow<List<Paper>> = paperDao.getPopularPapers(12)
        .map { list -> list.map { it.toDomain() } }

    val recentPapers: Flow<List<Paper>> = paperDao.getRecentPapers(10)
        .map { list -> list.map { it.toDomain() } }

    val favoritePapers: Flow<List<Paper>> = paperDao.getFavoritePapers()
        .map { list -> list.map { it.toDomain() } }

    val downloadedPapers: Flow<List<Paper>> = paperDao.getDownloadedPapers()
        .map { list -> list.map { it.toDomain() } }

    val allUniversitiesFlow: Flow<List<University>> = refDao.getAllUniversities()
        .map { list -> list.map { it.toDomain() } }

    val allCoursesFlow: Flow<List<Course>> = refDao.getAllCourses()
        .map { list -> list.map { it.toDomain() } }

    val allSemestersFlow: Flow<List<Semester>> = refDao.getAllSemesters()
        .map { list -> list.map { it.toDomain() } }

    val allSubjectsFlow: Flow<List<Subject>> = refDao.getAllSubjects()
        .map { list -> list.map { it.toDomain() } }

    val allYearsFlow: Flow<List<YearItem>> = refDao.getAllYears()
        .map { list -> list.map { it.toDomain() } }

    val allPaperTypesFlow: Flow<List<PaperType>> = refDao.getAllPaperTypes()
        .map { list -> list.map { it.toDomain() } }

    val allLanguagesFlow: Flow<List<LanguageItem>> = refDao.getAllLanguages()
        .map { list -> list.map { it.toDomain() } }

    val examSchedulesFlow: Flow<List<ExamSchedule>> = refDao.getAllExamSchedules()
        .map { list -> list.map { it.toDomain() } }

    fun getPaperFlow(id: String): Flow<Paper?> = paperDao.getPaperFlow(id)
        .map { it?.toDomain() }

    fun getPapersByUniversityFlow(uniId: String): Flow<List<Paper>> =
        paperDao.getPapersByUniversity(uniId).map { list -> list.map { it.toDomain() } }

    // ----------------------------------------------------
    // FIRESTORE SYNC & QUERIES (READ ONLY)
    // ----------------------------------------------------

    suspend fun syncReferenceData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var anySynced = false
            var uniEntities = emptyList<UniversityEntity>()
            var semEntities = emptyList<SemesterEntity>()

            // 1. Universities
            try {
                val uniSnap = firestore.collection("universities").get().await()
                uniEntities = uniSnap.documents.map { doc ->
                    UniversityEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id,
                        shortName = doc.getString("short_name") ?: doc.getString("shortName") ?: "",
                        description = doc.getString("description") ?: "",
                        logo = doc.getString("logo") ?: ""
                    )
                }
                if (uniEntities.isNotEmpty()) {
                    refDao.insertUniversities(uniEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Ignore collection-specific failure
            }

            // 2. Courses
            try {
                val courseSnap = firestore.collection("courses").get().await()
                val courseEntities = courseSnap.documents.map { doc ->
                    CourseEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id
                    )
                }
                if (courseEntities.isNotEmpty()) {
                    refDao.insertCourses(courseEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 3. Semesters
            try {
                val semSnap = firestore.collection("semesters").get().await()
                semEntities = semSnap.documents.map { doc ->
                    SemesterEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id
                    )
                }
                if (semEntities.isNotEmpty()) {
                    refDao.insertSemesters(semEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 4. Subjects
            try {
                val subSnap = firestore.collection("subjects").get().await()
                val subEntities = subSnap.documents.map { doc ->
                    SubjectEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id,
                        code = doc.getString("code") ?: ""
                    )
                }
                if (subEntities.isNotEmpty()) {
                    refDao.insertSubjects(subEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 5. Years
            try {
                val yearSnap = firestore.collection("years").get().await()
                val yearEntities = yearSnap.documents.map { doc ->
                    val yearVal = doc.get("year")?.toString() ?: doc.getString("name") ?: doc.id
                    YearEntity(id = doc.id, year = yearVal)
                }
                if (yearEntities.isNotEmpty()) {
                    refDao.insertYears(yearEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 6. Paper Types
            try {
                val ptSnap = firestore.collection("paper_types").get().await()
                val ptEntities = ptSnap.documents.map { doc ->
                    PaperTypeEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id
                    )
                }
                if (ptEntities.isNotEmpty()) {
                    refDao.insertPaperTypes(ptEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 7. Languages
            try {
                val langSnap = firestore.collection("languages").get().await()
                val langEntities = langSnap.documents.map { doc ->
                    LanguageEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id
                    )
                }
                if (langEntities.isNotEmpty()) {
                    refDao.insertLanguages(langEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 8. Exam Schedules
            try {
                val examSnap = firestore.collection("exam_schedules").get().await()
                val uniMap = uniEntities.associate { it.id to it.name }
                val semMap = semEntities.associate { it.id to it.name }
                val scheduleEntities = examSnap.documents.map { doc ->
                    val uId = doc.getString("universityId") ?: ""
                    val sId = doc.getString("semesterId") ?: ""
                    ExamScheduleEntity(
                        id = doc.id,
                        universityId = uId,
                        universityName = uniMap[uId] ?: uId,
                        semesterId = sId,
                        semesterName = semMap[sId] ?: sId,
                        startDateMillis = doc.getTimestamp("startDate")?.toDate()?.time,
                        endDateMillis = doc.getTimestamp("endDate")?.toDate()?.time,
                        academicYear = doc.getString("academicYear") ?: ""
                    )
                }
                if (scheduleEntities.isNotEmpty()) {
                    refDao.insertExamSchedules(scheduleEntities)
                    anySynced = true
                }
            } catch (e: Exception) {
                // Non-fatal if exam_schedules is empty or missing
            }

            if (anySynced) Result.success(Unit) else Result.failure(Exception("No reference collections available"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPublishedPapers(limit: Long = 1000): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val snapshot = try {
                firestore.collection("papers")
                    .whereEqualTo("status", "published")
                    .limit(limit)
                    .get()
                    .await()
            } catch (_: Exception) {
                firestore.collection("papers")
                    .limit(limit)
                    .get()
                    .await()
            }

            val entities = snapshot.documents.map { doc ->
                val yearVal = doc.get("year")?.toString() ?: ""
                val viewCountVal = (doc.get("viewCount") as? Number)?.toLong() ?: 0L

                PaperEntity(
                    id = doc.id,
                    title = doc.getString("title") ?: "Untitled Paper",
                    pdfUrl = doc.getString("pdfUrl") ?: "",
                    universityId = doc.getString("universityId") ?: "",
                    universityName = doc.getString("universityName") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    courseName = doc.getString("courseName") ?: "",
                    semesterId = doc.getString("semesterId") ?: "",
                    semesterName = doc.getString("semesterName") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    yearId = doc.getString("yearId") ?: "",
                    year = yearVal,
                    paperTypeId = doc.getString("paperTypeId") ?: "",
                    paperTypeName = doc.getString("paperTypeName") ?: "",
                    languageId = doc.getString("languageId") ?: "",
                    languageName = doc.getString("languageName") ?: "",
                    status = doc.getString("status") ?: "published",
                    description = doc.getString("description") ?: "",
                    viewCount = viewCountVal,
                    createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time
                )
            }

            paperDao.upsertPreservingLocalState(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPaperDetails(paperId: String): Paper? = withContext(Dispatchers.IO) {
        val local = paperDao.getPaperDirect(paperId)
        if (local != null) return@withContext local.toDomain()

        try {
            val doc = firestore.collection("papers").document(paperId).get().await()
            if (doc.exists()) {
                val yearVal = doc.get("year")?.toString() ?: ""
                val viewCountVal = (doc.get("viewCount") as? Number)?.toLong() ?: 0L
                val entity = PaperEntity(
                    id = doc.id,
                    title = doc.getString("title") ?: "Untitled Paper",
                    pdfUrl = doc.getString("pdfUrl") ?: "",
                    universityId = doc.getString("universityId") ?: "",
                    universityName = doc.getString("universityName") ?: "",
                    courseId = doc.getString("courseId") ?: "",
                    courseName = doc.getString("courseName") ?: "",
                    semesterId = doc.getString("semesterId") ?: "",
                    semesterName = doc.getString("semesterName") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    yearId = doc.getString("yearId") ?: "",
                    year = yearVal,
                    paperTypeId = doc.getString("paperTypeId") ?: "",
                    paperTypeName = doc.getString("paperTypeName") ?: "",
                    languageId = doc.getString("languageId") ?: "",
                    languageName = doc.getString("languageName") ?: "",
                    status = doc.getString("status") ?: "published",
                    description = doc.getString("description") ?: "",
                    viewCount = viewCountVal,
                    createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time
                )
                paperDao.insertPaper(entity)
                return@withContext entity.toDomain()
            }
        } catch (e: Exception) {
            // Offline or error
        }
        null
    }

    // ----------------------------------------------------
    // LOCAL STATE MODIFIERS (ROOM ONLY, ZERO FIRESTORE WRITES)
    // ----------------------------------------------------

    suspend fun toggleFavorite(paperId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        paperDao.updateFavorite(paperId, isFavorite)
    }

    suspend fun savePersonalNotes(paperId: String, notes: String) = withContext(Dispatchers.IO) {
        paperDao.updateNotes(paperId, notes)
    }

    suspend fun recordLocalPaperView(paperId: String) = withContext(Dispatchers.IO) {
        paperDao.updateViewTimestamp(paperId, System.currentTimeMillis())
    }
}
