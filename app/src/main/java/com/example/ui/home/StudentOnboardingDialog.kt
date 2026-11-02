package com.example.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.Semester
import com.example.data.model.Subject
import com.example.data.model.University

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentOnboardingDialog(
    universities: List<University>,
    semesters: List<Semester>,
    subjects: List<Subject>,
    initialUniId: String? = null,
    initialSemId: String? = null,
    initialSubId: String? = null,
    onSave: (
        uniId: String?,
        uniName: String?,
        semId: String?,
        semName: String?,
        subId: String?,
        subName: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUniId by remember { mutableStateOf(initialUniId) }
    var selectedSemId by remember { mutableStateOf(initialSemId) }
    var selectedSubId by remember { mutableStateOf(initialSubId) }

    var uniExpanded by remember { mutableStateOf(false) }
    var semExpanded by remember { mutableStateOf(false) }
    var subExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("onboarding_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "PYQ Logo",
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Select University
                val currentUniLabel = universities.firstOrNull { it.id == selectedUniId }?.name
                    ?: stringResource(R.string.select_your_university)
                ExposedDropdownMenuBox(
                    expanded = uniExpanded,
                    onExpandedChange = { uniExpanded = !uniExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentUniLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.university_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = uniExpanded,
                        onDismissRequest = { uniExpanded = false }
                    ) {
                        universities.forEach { uni ->
                            DropdownMenuItem(
                                text = { Text(uni.name) },
                                onClick = {
                                    selectedUniId = uni.id
                                    uniExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Select Semester
                val currentSemLabel = semesters.firstOrNull { it.id == selectedSemId }?.name
                    ?: stringResource(R.string.select_your_semester)
                ExposedDropdownMenuBox(
                    expanded = semExpanded,
                    onExpandedChange = { semExpanded = !semExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentSemLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.semester_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = semExpanded,
                        onDismissRequest = { semExpanded = false }
                    ) {
                        semesters.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text(sem.name) },
                                onClick = {
                                    selectedSemId = sem.id
                                    semExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Select Subject
                val currentSubLabel = subjects.firstOrNull { it.id == selectedSubId }?.name
                    ?: stringResource(R.string.select_your_subject)
                ExposedDropdownMenuBox(
                    expanded = subExpanded,
                    onExpandedChange = { subExpanded = !subExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentSubLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.subject_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = subExpanded,
                        onDismissRequest = { subExpanded = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    selectedSubId = sub.id
                                    subExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification feature highlight badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.exam_alerts_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val uniName = universities.firstOrNull { it.id == selectedUniId }?.name
                        val semName = semesters.firstOrNull { it.id == selectedSemId }?.name
                        val subName = subjects.firstOrNull { it.id == selectedSubId }?.name
                        onSave(selectedUniId, uniName, selectedSemId, semName, selectedSubId, subName)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_onboarding_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.save_and_continue), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.skip_setup))
                }
            }
        }
    }
}
