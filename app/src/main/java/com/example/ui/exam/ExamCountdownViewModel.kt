package com.example.ui.exam

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ExamSchedule
import com.example.data.repository.ArchiveRepository
import com.example.worker.ExamReminderReceiver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Locale

class ExamCountdownViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArchiveRepository(application)

    val examSchedules: StateFlow<List<ExamSchedule>> = repository.examSchedulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scheduleExamReminder(schedule: ExamSchedule, daysBefore: Int = 1): Result<String> {
        val context = getApplication<Application>()
        val startMillis = schedule.startDate?.toDate()?.time ?: return Result.failure(Exception("Invalid start date"))

        val reminderTimeMillis = startMillis - (daysBefore * 24L * 60L * 60L * 1000L)
        if (reminderTimeMillis <= System.currentTimeMillis()) {
            return Result.failure(Exception("Cannot set reminder for past dates"))
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ExamReminderReceiver::class.java).apply {
            putExtra(ExamReminderReceiver.EXTRA_EXAM_TITLE, "Upcoming Exam: ${schedule.universityName}")
            putExtra(
                ExamReminderReceiver.EXTRA_EXAM_MESSAGE,
                "Exams for ${schedule.semesterName} start in $daysBefore day(s)."
            )
            putExtra(ExamReminderReceiver.EXTRA_NOTIFICATION_ID, schedule.id.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeMillis,
                    pendingIntent
                )
            }
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(reminderTimeMillis)
            Result.success(dateStr)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
