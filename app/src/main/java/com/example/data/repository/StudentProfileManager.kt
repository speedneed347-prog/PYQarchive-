package com.example.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.ExamSchedule
import com.example.data.model.StudentProfile
import com.example.worker.ExamReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class StudentProfileManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("student_profile_prefs", Context.MODE_PRIVATE)

    private val _profileFlow = MutableStateFlow(loadProfileFromPrefs())
    val profileFlow: StateFlow<StudentProfile> = _profileFlow.asStateFlow()

    private fun loadProfileFromPrefs(): StudentProfile {
        return StudentProfile(
            universityId = prefs.getString(KEY_UNI_ID, null),
            universityName = prefs.getString(KEY_UNI_NAME, null),
            semesterId = prefs.getString(KEY_SEM_ID, null),
            semesterName = prefs.getString(KEY_SEM_NAME, null),
            subjectId = prefs.getString(KEY_SUB_ID, null),
            subjectName = prefs.getString(KEY_SUB_NAME, null),
            isOnboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        )
    }

    fun saveProfile(
        universityId: String?,
        universityName: String?,
        semesterId: String?,
        semesterName: String?,
        subjectId: String?,
        subjectName: String?
    ) {
        prefs.edit()
            .putString(KEY_UNI_ID, universityId)
            .putString(KEY_UNI_NAME, universityName)
            .putString(KEY_SEM_ID, semesterId)
            .putString(KEY_SEM_NAME, semesterName)
            .putString(KEY_SUB_ID, subjectId)
            .putString(KEY_SUB_NAME, subjectName)
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()

        _profileFlow.value = loadProfileFromPrefs()
    }

    fun skipOnboarding() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
        _profileFlow.value = loadProfileFromPrefs()
    }

    /**
     * Schedules automated notifications for this student's exam schedule:
     * 7 days before, 3 days before, 1 day before, and the morning of the exam.
     */
    fun scheduleAutomatedExamAlerts(schedule: ExamSchedule) {
        try {
            val startMillis = schedule.startDate?.toDate()?.time ?: return
            val now = System.currentTimeMillis()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intervals = listOf(
                7 to "Your exams start in 1 week. Prepare your PYQ revision!",
                3 to "Your exams start in 3 days. Focus on high-yield topics!",
                1 to "Your exams start tomorrow. All the best for your paper!",
                0 to "Your exams start today! Stay confident and calm."
            )

            intervals.forEach { (daysBefore, alertMsg) ->
                val targetTimeMillis = if (daysBefore > 0) {
                    startMillis - (daysBefore * 24L * 60L * 60L * 1000L)
                } else {
                    // Morning of exam: 8:00 AM on exam start date
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = startMillis
                        set(Calendar.HOUR_OF_DAY, 8)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    cal.timeInMillis
                }

                if (targetTimeMillis > now) {
                    val intent = Intent(context, ExamReminderReceiver::class.java).apply {
                        putExtra(ExamReminderReceiver.EXTRA_EXAM_TITLE, "Upcoming Exam: ${schedule.universityName}")
                        putExtra(ExamReminderReceiver.EXTRA_EXAM_MESSAGE, alertMsg)
                        putExtra(ExamReminderReceiver.EXTRA_NOTIFICATION_ID, (schedule.id + daysBefore).hashCode())
                    }

                    val requestCode = (schedule.id + daysBefore).hashCode()
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    targetTimeMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    targetTimeMillis,
                                    pendingIntent
                                )
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetTimeMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                targetTimeMillis,
                                pendingIntent
                            )
                        }
                    } catch (_: Throwable) {
                        // Safe fallback: never crash app on alarm scheduling
                    }
                }
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    companion object {
        private const val KEY_UNI_ID = "pref_student_uni_id"
        private const val KEY_UNI_NAME = "pref_student_uni_name"
        private const val KEY_SEM_ID = "pref_student_sem_id"
        private const val KEY_SEM_NAME = "pref_student_sem_name"
        private const val KEY_SUB_ID = "pref_student_sub_id"
        private const val KEY_SUB_NAME = "pref_student_sub_name"
        private const val KEY_ONBOARDING_DONE = "pref_student_onboarding_done"

        @Volatile
        private var INSTANCE: StudentProfileManager? = null

        fun getInstance(context: Context): StudentProfileManager {
            return INSTANCE ?: synchronized(this) {
                val instance = StudentProfileManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
