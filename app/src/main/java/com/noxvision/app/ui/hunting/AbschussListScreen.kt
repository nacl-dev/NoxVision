package com.noxvision.app.ui.hunting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        text = "Jagdtagebuch",
                        color = NightColors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Zurueck",
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
                                    contentDescription = "Exportieren",
                                    tint = NightColors.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Als CSV exportieren") },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            val exporter = CsvExporter(context)
                                            val result = exporter.exportRecords(records)
                                            exportMessage = if (result.isSuccess) {
                                                "CSV gespeichert: ${result.getOrNull()}"
                                            } else {
                                                "Fehler: ${result.exceptionOrNull()?.message}"
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.TableChart, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Als PDF exportieren") },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            val exporter = PdfExporter(context)
                                            val result = exporter.exportRecords(records)
                                            exportMessage = if (result.isSuccess) {
                                                "PDF gespeichert: ${result.getOrNull()}"
                                            } else {
                                                "Fehler: ${result.exceptionOrNull()?.message}"
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
                Icon(Icons.Filled.Add, contentDescription = "Neuer Eintrag")
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
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = NightColors.onBackground,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Noch keine Eintraege",
                        color = NightColors.onBackground,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tippe auf + um einen Abschuss zu dokumentieren",
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
            title = { Text("Eintrag loeschen?") },
            text = { Text("Dieser Eintrag wird unwiderruflich geloescht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            db.huntRecordDao().deleteById(recordId)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("Loeschen", color = NightColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Abbrechen")
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
                    Text("OK")
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
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY) }

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
                    contentDescription = "Loeschen",
                    tint = NightColors.error
                )
            }
        }
    }
}
