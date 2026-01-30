package com.noxvision.app.ui.hunting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.R
import com.noxvision.app.hunting.calendar.HuntingSeasonData
import com.noxvision.app.hunting.database.HuntingDatabase
import com.noxvision.app.hunting.moon.MoonPhaseCalculator
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.SettingsSectionHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class HuntingScreen {
    HUB,
    ABSCHUSS_LIST,
    ABSCHUSS_FORM,
    NACHSUCHE,
    MAP,
    CALENDAR,
    WEATHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HuntingHubScreen(
    onClose: () -> Unit
) {
    var currentScreen by rememberSaveable { mutableStateOf(HuntingScreen.HUB) }
    var selectedRecordId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Intercept back button to navigate internally
    androidx.activity.compose.BackHandler(enabled = currentScreen != HuntingScreen.HUB) {
        if (currentScreen == HuntingScreen.ABSCHUSS_FORM) {
            // Special case: form goes back to list
            selectedRecordId = null
            currentScreen = HuntingScreen.ABSCHUSS_LIST
        } else {
            // All others go back to HUB
            currentScreen = HuntingScreen.HUB
        }
    }

    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NightColors.background
    ) {
        when (currentScreen) {
            HuntingScreen.HUB -> HuntingHubContent(
                onClose = onClose,
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
            HuntingScreen.ABSCHUSS_LIST -> AbschussListScreen(
                onBack = { currentScreen = HuntingScreen.HUB },
                onAddNew = { currentScreen = HuntingScreen.ABSCHUSS_FORM },
                onEdit = { id ->
                    selectedRecordId = id
                    currentScreen = HuntingScreen.ABSCHUSS_FORM
                }
            )
            HuntingScreen.ABSCHUSS_FORM -> AbschussFormScreen(
                recordId = selectedRecordId,
                onBack = {
                    selectedRecordId = null
                    currentScreen = HuntingScreen.ABSCHUSS_LIST
                },
                onSaved = {
                    selectedRecordId = null
                    currentScreen = HuntingScreen.ABSCHUSS_LIST
                }
            )
            HuntingScreen.NACHSUCHE -> NachsucheScreen(
                onBack = { currentScreen = HuntingScreen.HUB }
            )
            HuntingScreen.MAP -> MapScreen(
                onBack = { currentScreen = HuntingScreen.HUB }
            )
            HuntingScreen.CALENDAR -> CalendarScreen(
                onBack = { currentScreen = HuntingScreen.HUB }
            )
            HuntingScreen.WEATHER -> WeatherScreen(
                onBack = { currentScreen = HuntingScreen.HUB }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HuntingHubContent(
    onClose: () -> Unit,
    onNavigate: (HuntingScreen) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val moonInfo = remember { MoonPhaseCalculator.calculateMoonPhase() }
    var recordCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val db = HuntingDatabase.getDatabase(context)
        recordCount = db.huntRecordDao().getCount()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hunting_assistant),
                        color = NightColors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Info Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Moon Phase Card
                QuickInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = moonInfo.phase.icon,
                    title = moonInfo.phase.germanName,
                    subtitle = moonInfo.activityPrediction.germanText
                )

                // Records Count Card
                QuickInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = "\uD83D\uDCDD",
                    title = stringResource(R.string.entries, recordCount),
                    subtitle = stringResource(R.string.hunting_diary)
                )
            }

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 8.dp))

            // Main Features
            SettingsSectionHeader(
                icon = {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                title = stringResource(R.string.main_features)
            )

            HuntingFeatureCard(
                icon = Icons.Filled.EditNote,
                title = stringResource(R.string.kill_documentation),
                subtitle = stringResource(R.string.kill_documentation_subtitle),
                onClick = { onNavigate(HuntingScreen.ABSCHUSS_LIST) }
            )

            HuntingFeatureCard(
                icon = Icons.Filled.Explore,
                title = stringResource(R.string.tracking),
                subtitle = stringResource(R.string.tracking_subtitle),
                onClick = { onNavigate(HuntingScreen.NACHSUCHE) }
            )

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 8.dp))

            // Info Features
            SettingsSectionHeader(
                icon = {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                title = stringResource(R.string.information)
            )

            HuntingFeatureCard(
                icon = Icons.Filled.Cloud,
                title = stringResource(R.string.weather),
                subtitle = stringResource(R.string.weather_subtitle),
                onClick = { onNavigate(HuntingScreen.WEATHER) }
            )

            HuntingFeatureCard(
                icon = Icons.Filled.CalendarMonth,
                title = stringResource(R.string.hunting_seasons),
                subtitle = stringResource(R.string.hunting_seasons_subtitle),
                onClick = { onNavigate(HuntingScreen.CALENDAR) }
            )

            HuntingFeatureCard(
                icon = Icons.Filled.Map,
                title = stringResource(R.string.map),
                subtitle = stringResource(R.string.map_subtitle),
                onClick = { onNavigate(HuntingScreen.MAP) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuickInfoCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NightColors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = NightColors.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = NightColors.onBackground,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HuntingFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NightColors.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NightColors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = NightColors.onBackground,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = NightColors.onBackground
            )
        }
    }
}
