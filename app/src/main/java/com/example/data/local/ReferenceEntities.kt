package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.data.model.Course
import com.example.data.model.ExamSchedule
import com.example.data.model.LanguageItem
import com.example.data.model.PaperType
import com.example.data.model.Semester
import com.example.data.model.Subject
import com.example.data.model.University
import com.example.data.model.YearItem
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "universities")
data class UniversityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val description: String,
    val logo: String
) {
    fun toDomain() = University(id, name, shortName, description, logo)
}

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val name: String
) {
    fun toDomain() = Course(id, name)
}

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey val id: String,
    val name: String
) {
    fun toDomain() = Semester(id, name)
}

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String
) {
    fun toDomain() = Subject(id, name, code)
}

@Entity(tableName = "years")
data class YearEntity(
    @PrimaryKey val id: String,
    val year: String
) {
    fun toDomain() = YearItem(id, year)
}

@Entity(tableName = "paper_types")
data class PaperTypeEntity(
    @PrimaryKey val id: String,
    val name: String
) {
    fun toDomain() = PaperType(id, name)
}

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey val id: String,
    val name: String
) {
    fun toDomain() = LanguageItem(id, name)
}

@Entity(tableName = "exam_schedules")
data class ExamScheduleEntity(
    @PrimaryKey val id: String,
    val universityId: String,
    val universityName: String,
    val semesterId: String,
    val semesterName: String,
    val startDateMillis: Long?,
    val endDateMillis: Long?,
    val academicYear: String
) {
    fun toDomain(): ExamSchedule {
        return ExamSchedule(
            id = id,
            universityId = universityId,
            universityName = universityName,
            semesterId = semesterId,
            semesterName = semesterName,
            startDate = startDateMillis?.let { Timestamp(Date(it)) },
            endDate = endDateMillis?.let { Timestamp(Date(it)) },
            academicYear = academicYear
        )
    }
}

@Dao
interface ReferenceDao {
    @Query("SELECT * FROM universities ORDER BY name ASC")
    fun getAllUniversities(): Flow<List<UniversityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUniversities(items: List<UniversityEntity>)

    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(items: List<CourseEntity>)

    @Query("SELECT * FROM semesters ORDER BY name ASC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesters(items: List<SemesterEntity>)

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(items: List<SubjectEntity>)

    @Query("SELECT * FROM years ORDER BY year DESC")
    fun getAllYears(): Flow<List<YearEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYears(items: List<YearEntity>)

    @Query("SELECT * FROM paper_types ORDER BY name ASC")
    fun getAllPaperTypes(): Flow<List<PaperTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaperTypes(items: List<PaperTypeEntity>)

    @Query("SELECT * FROM languages ORDER BY name ASC")
    fun getAllLanguages(): Flow<List<LanguageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguages(items: List<LanguageEntity>)

    @Query("SELECT * FROM exam_schedules ORDER BY startDateMillis ASC")
    fun getAllExamSchedules(): Flow<List<ExamScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSchedules(items: List<ExamScheduleEntity>)
}
