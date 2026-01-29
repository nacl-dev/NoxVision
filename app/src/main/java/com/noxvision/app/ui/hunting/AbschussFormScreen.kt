package com.noxvision.app.ui.hunting

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.noxvision.app.hunting.calendar.HuntingSeasonData
import com.noxvision.app.hunting.database.HuntingDatabase
import com.noxvision.app.hunting.database.entities.HuntRecord
import com.noxvision.app.hunting.database.entities.WildlifeTypes
import com.noxvision.app.hunting.location.HuntingLocationManager
import com.noxvision.app.hunting.moon.MoonPhaseCalculator
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.SettingsSectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbschussFormScreen(
    recordId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { HuntingDatabase.getDatabase(context) }
    val locationManager = remember { HuntingLocationManager(context) }

    var wildlifeType by remember { mutableStateOf("Rehwild") }
    var gender by remember { mutableStateOf<String?>(null) }
    var estimatedWeight by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var bundesland by remember { mutableStateOf(HuntingSeasonData.Bundesland.BAYERN) }
    var isLoading by remember { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    var wildlifeTypeExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var bundeslandExpanded by remember { mutableStateOf(false) }

    val genderOptions = WildlifeTypes.getGendersForType(wildlifeType)

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoUri = it }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            locationLoading = true
            scope.launch {
                val location = locationManager.getCurrentLocation()
                location?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                }
                locationLoading = false
            }
        }
    }

    // Load existing record if editing
    LaunchedEffect(recordId) {
        if (recordId != null) {
            val record = db.huntRecordDao().getRecordById(recordId)
            record?.let {
                wildlifeType = it.wildlifeType
                gender = it.gender
                estimatedWeight = it.estimatedWeight?.toString() ?: ""
                notes = it.notes ?: ""
                latitude = it.latitude
                longitude = it.longitude
                it.bundesland?.let { bl ->
                    HuntingSeasonData.Bundesland.entries.find { b -> b.displayName == bl }?.let { found ->
                        bundesland = found
                    }
                }
                it.thermalImagePath?.let { path ->
                    photoUri = Uri.parse(path)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (recordId == null) "Neuer Abschuss" else "Abschuss bearbeiten",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightColors.background
                )
            )
        },
        containerColor = NightColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Wildlife Type
            SettingsSectionHeader(
                icon = {
                    Icon(
                        Icons.Filled.Pets,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                title = "WILD"
            )

            ExposedDropdownMenuBox(
                expanded = wildlifeTypeExpanded,
                onExpandedChange = { wildlifeTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = wildlifeType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Wildart") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wildlifeTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = huntingTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = wildlifeTypeExpanded,
                    onDismissRequest = { wildlifeTypeExpanded = false }
                ) {
                    WildlifeTypes.ALL_TYPES.keys.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                wildlifeType = type
                                gender = null
                                wildlifeTypeExpanded = false
                            }
                        )
                    }
                }
            }

            if (genderOptions.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = it }
                ) {
                    OutlinedTextField(
                        value = gender ?: "Nicht angegeben",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Geschlecht/Alter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = huntingTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    gender = option
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = estimatedWeight,
                onValueChange = { if (it.all { c -> c.isDigit() }) estimatedWeight = it },
                label = { Text("Geschaetztes Gewicht (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = huntingTextFieldColors()
            )

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

            // Location
            SettingsSectionHeader(
                icon = {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                title = "POSITION"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationLoading = true
                            scope.launch {
                                val location = locationManager.getCurrentLocation()
                                location?.let {
                                    latitude = it.latitude
                                    longitude = it.longitude
                                }
                                locationLoading = false
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !locationLoading
                ) {
                    if (locationLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NightColors.onSurface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GPS erfassen")
                }

                if (latitude != null && longitude != null) {
                    Text(
                        text = String.format("%.5f, %.5f", latitude, longitude),
                        color = NightColors.success,
                        fontSize = 12.sp
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = bundeslandExpanded,
                onExpandedChange = { bundeslandExpanded = it }
            ) {
                OutlinedTextField(
                    value = bundesland.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bundesland") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bundeslandExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = huntingTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = bundeslandExpanded,
                    onDismissRequest = { bundeslandExpanded = false }
                ) {
                    HuntingSeasonData.Bundesland.entries.forEach { bl ->
                        DropdownMenuItem(
                            text = { Text(bl.displayName) },
                            onClick = {
                                bundesland = bl
                                bundeslandExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

            // Photo
            SettingsSectionHeader(
                icon = {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                title = "FOTO"
            )

            if (photoUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NightColors.surface, RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { photoUri = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Foto entfernen",
                            tint = NightColors.error
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NightColors.surface)
                        .border(1.dp, NightColors.onBackground.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = NightColors.onBackground,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Foto hinzufuegen",
                            color = NightColors.onBackground,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

            // Notes
            SettingsSectionHeader(
                icon = {
                    Icon(
                        Icons.Filled.Notes,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                title = "NOTIZEN"
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Zusaetzliche Notizen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = huntingTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        val moonInfo = MoonPhaseCalculator.calculateMoonPhase()

                        val record = HuntRecord(
                            id = recordId ?: 0,
                            timestamp = System.currentTimeMillis(),
                            latitude = latitude,
                            longitude = longitude,
                            wildlifeType = wildlifeType,
                            gender = gender,
                            estimatedWeight = estimatedWeight.toIntOrNull(),
                            notes = notes.ifBlank { null },
                            thermalImagePath = photoUri?.toString(),
                            moonPhase = moonInfo.phase.germanName,
                            weatherSnapshot = null,
                            bundesland = bundesland.displayName
                        )

                        if (recordId != null) {
                            db.huntRecordDao().update(record)
                        } else {
                            db.huntRecordDao().insert(record)
                        }

                        isLoading = false
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = NightColors.onSurface,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speichern")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun huntingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NightColors.primary,
    unfocusedBorderColor = NightColors.onBackground,
    focusedLabelColor = NightColors.primary,
    unfocusedLabelColor = NightColors.onBackground,
    cursorColor = NightColors.primary,
    focusedTextColor = NightColors.onSurface,
    unfocusedTextColor = NightColors.onSurface
)
