package com.noxvision.app.ui.hunting

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.noxvision.app.hunting.database.HuntingDatabase
import com.noxvision.app.hunting.database.entities.Waypoint
import com.noxvision.app.hunting.database.entities.WaypointType
import com.noxvision.app.hunting.location.CompassData
import com.noxvision.app.hunting.location.CompassSensor
import com.noxvision.app.hunting.location.HuntingLocationManager
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.SettingsSectionHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NachsucheScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { HuntingDatabase.getDatabase(context) }
    val locationManager = remember { HuntingLocationManager(context) }
    val compassSensor = remember { CompassSensor(context) }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var compassData by remember { mutableStateOf<CompassData?>(null) }
    var selectedWaypoint by remember { mutableStateOf<Waypoint?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(WaypointType.ANSCHUSS) }

    val waypoints by db.waypointDao().getStandaloneWaypoints().collectAsState(initial = emptyList())

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                locationManager.getLocationUpdates(2000).collect { location ->
                    currentLocation = location
                }
            }
        }
    }

    // Start compass and location updates
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                locationManager.getLocationUpdates(2000).collect { location ->
                    currentLocation = location
                }
            }
        }

        if (compassSensor.isAvailable) {
            scope.launch {
                compassSensor.getCompassUpdates().collect { data ->
                    compassData = data
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
                        text = "Nachsuche",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        showAddDialog = true
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                containerColor = NightColors.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(Icons.Filled.AddLocation, contentDescription = "Wegpunkt hinzufuegen")
            }
        },
        containerColor = NightColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Compass Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (compassSensor.isAvailable && compassData != null) {
                        val bearing = selectedWaypoint?.let { wp ->
                            currentLocation?.let { loc ->
                                HuntingLocationManager.calculateBearing(
                                    loc.latitude, loc.longitude,
                                    wp.latitude, wp.longitude
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TrackingCompass(
                                compassAzimuth = compassData!!.azimuth,
                                targetBearing = bearing
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${compassData!!.azimuth.toInt()}\u00B0 ${compassData!!.direction}",
                            color = NightColors.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (selectedWaypoint != null && currentLocation != null) {
                            val distance = HuntingLocationManager.calculateDistance(
                                currentLocation!!.latitude, currentLocation!!.longitude,
                                selectedWaypoint!!.latitude, selectedWaypoint!!.longitude
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Entfernung: ${HuntingLocationManager.formatDistance(distance)}",
                                color = NightColors.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Ziel: ${getWaypointTypeName(selectedWaypoint!!.type)}",
                                color = NightColors.onBackground,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.ExploreOff,
                            contentDescription = null,
                            tint = NightColors.onBackground,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kompass nicht verfuegbar",
                            color = NightColors.onBackground,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current position
            if (currentLocation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = NightColors.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aktuelle Position",
                                color = NightColors.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = HuntingLocationManager.formatCoordinates(
                                    currentLocation!!.latitude,
                                    currentLocation!!.longitude
                                ),
                                color = NightColors.onBackground,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Genauigkeit: ${currentLocation!!.accuracy.toInt()}m",
                                color = NightColors.onBackground,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Waypoints list
            SettingsSectionHeader(
                icon = {
                    Icon(
                        Icons.Filled.PinDrop,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                title = "WEGPUNKTE"
            )

            if (waypoints.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Keine Wegpunkte",
                            color = NightColors.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tippe auf + um einen Anschuss oder Wegpunkt zu markieren",
                            color = NightColors.onBackground,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(waypoints, key = { it.id }) { waypoint ->
                        WaypointCard(
                            waypoint = waypoint,
                            isSelected = selectedWaypoint?.id == waypoint.id,
                            currentLocation = currentLocation,
                            onSelect = { selectedWaypoint = waypoint },
                            onDelete = {
                                scope.launch {
                                    db.waypointDao().delete(waypoint)
                                    if (selectedWaypoint?.id == waypoint.id) {
                                        selectedWaypoint = null
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Add waypoint dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Wegpunkt hinzufuegen") },
            text = {
                Column {
                    Text("Typ auswaehlen:")
                    Spacer(modifier = Modifier.height(12.dp))

                    WaypointType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getWaypointTypeName(type))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentLocation?.let { loc ->
                            scope.launch {
                                val waypoint = Waypoint(
                                    huntRecordId = null,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude,
                                    type = selectedType,
                                    timestamp = System.currentTimeMillis(),
                                    compassBearing = compassData?.azimuth,
                                    notes = null
                                )
                                db.waypointDao().insert(waypoint)
                            }
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun WaypointCard(
    waypoint: Waypoint,
    isSelected: Boolean,
    currentLocation: Location?,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.GERMANY) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NightColors.primary.copy(alpha = 0.2f) else NightColors.surface
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = getWaypointColor(waypoint.type),
                modifier = Modifier.size(12.dp)
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getWaypointTypeName(waypoint.type),
                    color = NightColors.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(Date(waypoint.timestamp)),
                    color = NightColors.onBackground,
                    fontSize = 12.sp
                )
                currentLocation?.let { loc ->
                    val distance = HuntingLocationManager.calculateDistance(
                        loc.latitude, loc.longitude,
                        waypoint.latitude, waypoint.longitude
                    )
                    Text(
                        text = HuntingLocationManager.formatDistance(distance),
                        color = NightColors.primary,
                        fontSize = 12.sp
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

@Composable
private fun TrackingCompass(
    compassAzimuth: Float,
    targetBearing: Float?
) {
    val primaryColor = NightColors.primary
    val backgroundColor = NightColors.onBackground
    val targetColor = NightColors.error

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Draw compass circle
        drawCircle(
            color = backgroundColor.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Draw cardinal direction markers (fixed, compass rotates)
        rotate(-compassAzimuth, center) {
            for (i in 0 until 8) {
                val angle = i * 45f
                rotate(angle, center) {
                    drawLine(
                        color = if (i == 0) primaryColor else backgroundColor,
                        start = Offset(center.x, center.y - radius + 8),
                        end = Offset(center.x, center.y - radius + if (i % 2 == 0) 20 else 14),
                        strokeWidth = if (i == 0) 4f else 2f
                    )
                }
            }

            // Draw N marker
            drawCircle(
                color = primaryColor,
                radius = 8f,
                center = Offset(center.x, center.y - radius + 28)
            )
        }

        // Draw target direction arrow if we have a target
        if (targetBearing != null) {
            val relativeBearing = targetBearing - compassAzimuth
            rotate(relativeBearing, center) {
                // Target arrow
                drawLine(
                    color = targetColor,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.7f),
                    strokeWidth = 6f
                )
                // Arrow head
                drawLine(
                    color = targetColor,
                    start = Offset(center.x, center.y - radius * 0.7f),
                    end = Offset(center.x - 12, center.y - radius * 0.5f),
                    strokeWidth = 6f
                )
                drawLine(
                    color = targetColor,
                    start = Offset(center.x, center.y - radius * 0.7f),
                    end = Offset(center.x + 12, center.y - radius * 0.5f),
                    strokeWidth = 6f
                )
            }
        }

        // Center dot
        drawCircle(
            color = primaryColor,
            radius = 6f,
            center = center
        )
    }
}

private fun getWaypointTypeName(type: WaypointType): String {
    return when (type) {
        WaypointType.ANSCHUSS -> "Anschuss"
        WaypointType.LAST_SEEN -> "Letzte Sichtung"
        WaypointType.BLOOD_TRAIL -> "Schweissfaehrte"
        WaypointType.RECOVERY -> "Fundstelle"
        WaypointType.CUSTOM -> "Benutzerdefiniert"
    }
}

private fun getWaypointColor(type: WaypointType): Color {
    return when (type) {
        WaypointType.ANSCHUSS -> Color(0xFFFF5722)
        WaypointType.LAST_SEEN -> Color(0xFFFFEB3B)
        WaypointType.BLOOD_TRAIL -> Color(0xFFF44336)
        WaypointType.RECOVERY -> Color(0xFF4CAF50)
        WaypointType.CUSTOM -> Color(0xFF2196F3)
    }
}
