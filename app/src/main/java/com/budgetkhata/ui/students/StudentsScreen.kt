package com.budgetkhata.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.R
import com.budgetkhata.core.Money
import com.budgetkhata.core.PhoneUtils
import com.budgetkhata.data.local.model.StudentGroup
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.EmptyState
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.components.SearchField
import com.budgetkhata.ui.theme.LocalStatusPalette

@Composable
fun StudentsScreen(
    container: AppContainer,
    onOpenStudent: (Long) -> Unit,
    onAddStudent: () -> Unit,
    onEditStudent: (Long) -> Unit,
    onOpenReports: () -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> StudentsViewModel(app, handle) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalStatusPalette.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.students)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddStudent,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_student), modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.search_student_hint)
                )
            }

            if (state.loading) {
                LoadingBox()
            } else if (state.groups.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_matching_students),
                    message = stringResource(R.string.no_matching_msg),
                    icon = Icons.Rounded.Search
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.groups, key = { it.studentId }) { group ->
                        SimpleStudentCard(
                            group = group,
                            palette = palette,
                            onClick = { onOpenStudent(group.studentId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleStudentCard(
    group: StudentGroup,
    palette: com.budgetkhata.ui.theme.StatusPalette,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials(group.name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.size(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = PhoneUtils.display(group.phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AmountInfo(
                    label = stringResource(R.string.total_contribution),
                    value = Money.format(group.requiredPaise),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AmountInfo(
                    label = stringResource(R.string.amount_paid),
                    value = Money.format(group.paidPaise),
                    color = palette.paid
                )
                
                // Detailed Color Logic: Orange for Partial, Red for none, Green for full
                val amountColor = when {
                    group.remainingPaise == 0L -> palette.paid
                    group.paidPaise > 0L -> palette.partial
                    else -> palette.pending
                }
                
                AmountInfo(
                    label = stringResource(R.string.pending_amount),
                    value = Money.format(group.remainingPaise),
                    color = amountColor
                )
            }
        }
    }
}

@Composable
private fun AmountInfo(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun initials(name: String): String = name
    .split(" ")
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .take(1)
    .joinToString("")
    .ifBlank { "?" }
