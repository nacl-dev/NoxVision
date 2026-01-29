package com.noxvision.app.ui.hunting

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.noxvision.app.hunting.database.HuntingDatabase
import com.noxvision.app.hunting.database.entities.HuntingStand
import com.noxvision.app.hunting.database.entities.HuntingStandType
import com.noxvision.app.hunting.location.HuntingLocationManager
import com.noxvision.app.hunting.maps.OfflineMapManager
import com.noxvision.app.ui.NightColors
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = remember { HuntingDatabase.getDatabase(context) }
    val mapManager = remember { OfflineMapManager(context) }
    val locationManager = remember { HuntingLocationManager(context) }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showAddStandDialog by remember { mutableStateOf(false) }
    var longPressLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var cacheSize by remember { mutableStateOf(mapManager.getCacheSize()) }

    val huntingStands by db.huntingStandDao().getAllStands().collectAsState(initial = emptyList())
    val waypoints by db.waypointDao().getAllWaypoints().collectAsState(initial = emptyList())

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            mapView?.let { map ->
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                locationOverlay.enableMyLocation()
                locationOverlay.enableFollowLocation()
                map.overlays.add(locationOverlay)
                map.invalidate()
            }
        }
    }

    // Lifecycle handling for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Update markers when data changes
    LaunchedEffect(huntingStands, waypoints) {
        mapView?.let { map ->
            // Remove existing markers (keep other overlays)
            map.overlays.removeAll { it is Marker }

            // Add hunting stand markers
            huntingStands.forEach { stand ->
                val marker = Marker(map)
                marker.position = GeoPoint(stand.latitude, stand.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = stand.name
                marker.snippet = getStandTypeName(stand.type)
                map.overlays.add(marker)
            }

            // Add waypoint markers
            waypoints.forEach { waypoint ->
                val marker = Marker(map)
                marker.position = GeoPoint(waypoint.latitude, waypoint.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.title = getWaypointTypeName(waypoint.type)
                map.overlays.add(marker)
            }

            map.invalidate()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Karte",
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
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                scope.launch {
                                    val location = locationManager.getCurrentLocation()
                                    location?.let { loc ->
                                        mapView?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                                        mapView?.controller?.setZoom(15.0)
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.MyLocation,
                            contentDescription = "Meine Position",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // OSMDroid MapView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(mapManager.getTileSource())
                        setMultiTouchControls(true)
                        controller.setZoom(OfflineMapManager.DEFAULT_ZOOM)
                        controller.setCenter(OfflineMapManager.DEFAULT_CENTER)
                        minZoomLevel = OfflineMapManager.MIN_ZOOM
                        maxZoomLevel = OfflineMapManager.MAX_ZOOM

                        // Add long press handler for adding hunting stands
                        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    longPressLocation = it
                                    showAddStandDialog = true
                                }
                                return true
                            }
                        })
                        overlays.add(0, mapEventsOverlay)

                        // Add location overlay if permission granted
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                            locationOverlay.enableMyLocation()
                            overlays.add(locationOverlay)
                        }

                        mapView = this
                    }
                },
                update = { }
            )

            // Zoom controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { mapView?.controller?.zoomIn() },
                    containerColor = NightColors.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Hineinzoomen")
                }
                FloatingActionButton(
                    onClick = { mapView?.controller?.zoomOut() },
                    containerColor = NightColors.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Herauszoomen")
                }
            }

            // Info card
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = NightColors.surface.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Hochsitze: ${huntingStands.size}",
                        color = NightColors.onSurface,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Wegpunkte: ${waypoints.size}",
                        color = NightColors.onSurface,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Cache: ${mapManager.formatCacheSize(cacheSize)}",
                        color = NightColors.onBackground,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Lang druecken um Hochsitz hinzuzufuegen",
                        color = NightColors.onBackground,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    // Add hunting stand dialog
    if (showAddStandDialog && longPressLocation != null) {
        var standName by remember { mutableStateOf("") }
        var standType by remember { mutableStateOf(HuntingStandType.HOCHSITZ) }
        var standTypeExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddStandDialog = false
                longPressLocation = null
            },
            title = { Text("Hochsitz hinzufuegen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = standName,
                        onValueChange = { standName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = standTypeExpanded,
                        onExpandedChange = { standTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = getStandTypeName(standType),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Typ") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = standTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = standTypeExpanded,
                            onDismissRequest = { standTypeExpanded = false }
                        ) {
                            HuntingStandType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(getStandTypeName(type)) },
                                    onClick = {
                                        standType = type
                                        standTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Position: ${String.format("%.5f", longPressLocation!!.latitude)}, ${String.format("%.5f", longPressLocation!!.longitude)}",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (standName.isNotBlank()) {
                            scope.launch {
                                val stand = HuntingStand(
                                    name = standName,
                                    latitude = longPressLocation!!.latitude,
                                    longitude = longPressLocation!!.longitude,
                                    type = standType,
                                    notes = null
                                )
                                db.huntingStandDao().insert(stand)
                            }
                        }
                        showAddStandDialog = false
                        longPressLocation = null
                    },
                    enabled = standName.isNotBlank()
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddStandDialog = false
                        longPressLocation = null
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

private fun getStandTypeName(type: HuntingStandType): String {
    return when (type) {
        HuntingStandType.HOCHSITZ -> "Hochsitz"
        HuntingStandType.KANZEL -> "Kanzel"
        HuntingStandType.DRUCKJAGD -> "Druckjagdstand"
        HuntingStandType.ANSITZ -> "Ansitz"
        HuntingStandType.CUSTOM -> "Benutzerdefiniert"
    }
}

private fun getWaypointTypeName(type: com.noxvision.app.hunting.database.entities.WaypointType): String {
    return when (type) {
        com.noxvision.app.hunting.database.entities.WaypointType.ANSCHUSS -> "Anschuss"
        com.noxvision.app.hunting.database.entities.WaypointType.LAST_SEEN -> "Letzte Sichtung"
        com.noxvision.app.hunting.database.entities.WaypointType.BLOOD_TRAIL -> "Schweissfaehrte"
        com.noxvision.app.hunting.database.entities.WaypointType.RECOVERY -> "Fundstelle"
        com.noxvision.app.hunting.database.entities.WaypointType.CUSTOM -> "Benutzerdefiniert"
    }
}
