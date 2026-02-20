package com.noxvision.app.ui.hunting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.R
import com.noxvision.app.hunting.database.HuntingDatabase
import com.noxvision.app.hunting.database.entities.HuntRecord
import com.noxvision.app.hunting.export.CsvExporter
import com.noxvision.app.hunting.export.PdfExporter
import com.noxvision.app.ui.NightColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbschussListScreen(
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { HuntingDatabase.getDatabase(context) }

    val records by db.huntRecordDao().getAllRecords().collectAsState(initial = emptyList())

    var showExportMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hunting_diary),
                        color = NightColors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = NightColors.onSurface
                        )
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.export),
                                    tint = NightColors.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_csv)) },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            val exporter = CsvExporter(context)
                                            val result = exporter.exportRecords(records)
                                            exportMessage = if (result.isSuccess) {
                                                context.getString(R.string.csv_saved, result.getOrNull().orEmpty())
                                            } else {
                                                "${context.getString(R.string.error)}: ${result.exceptionOrNull()?.message.orEmpty()}"
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.TableChart, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_pdf)) },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            val exporter = PdfExporter(context)
                                            val result = exporter.exportRecords(records)
                                            exportMessage = if (result.isSuccess) {
                                                context.getString(R.string.pdf_saved, result.getOrNull().orEmpty())
                                            } else {
                                                "${context.getString(R.string.error)}: ${result.exceptionOrNull()?.message.orEmpty()}"
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightColors.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNew,
                containerColor = NightColors.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_entry))
            }
        },
        containerColor = NightColors.background
    ) { paddingValues ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = NightColors.onBackground,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_entries),
                        color = NightColors.onBackground,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tap_to_add),
                        color = NightColors.onBackground,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    HuntRecordCard(
                        record = record,
                        onClick = { onEdit(record.id) },
                        onDelete = { showDeleteDialog = record.id }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { recordId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_entry)) },
            text = { Text(stringResource(R.string.delete_entry_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            db.huntRecordDao().deleteById(recordId)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = NightColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Export message snackbar
    exportMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            exportMessage = null
        }

        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { exportMessage = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        ) {
            Text(message)
        }
    }
}

@Composable
private fun HuntRecordCard(
    record: HuntRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NightColors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.wildlifeType} ${record.gender ?: ""}".trim(),
                    color = NightColors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(record.timestamp)),
                    color = NightColors.onBackground,
                    fontSize = 12.sp
                )
                if (record.estimatedWeight != null) {
                    Text(
                        text = "${record.estimatedWeight} kg",
                        color = NightColors.onBackground,
                        fontSize = 12.sp
                    )
                }
                if (record.bundesland != null) {
                    Text(
                        text = record.bundesland,
                        color = NightColors.primary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = NightColors.error
                )
            }
        }
    }
}
