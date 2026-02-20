package com.noxvision.app.ui.hunting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.R
import com.noxvision.app.hunting.calendar.HuntingSeasonData
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.SettingsSectionHeader
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit
) {
    var selectedBundesland by remember { mutableStateOf(HuntingSeasonData.Bundesland.BAYERN) }
    var bundeslandExpanded by remember { mutableStateOf(false) }
    var showOnlyInSeason by remember { mutableStateOf(false) }

    val allSeasons = remember(selectedBundesland) {
        HuntingSeasonData.getSeasonsForBundesland(selectedBundesland)
    }

    val inSeasonSeasons = remember(selectedBundesland) {
        HuntingSeasonData.getSeasonsInEffect(selectedBundesland)
    }

    val upcomingSeasons = remember(selectedBundesland) {
        HuntingSeasonData.getUpcomingSeasons(selectedBundesland, 30)
    }

    val displaySeasons = if (showOnlyInSeason) inSeasonSeasons else allSeasons

    // Group seasons by wildlife type
    val groupedSeasons = displaySeasons.groupBy { it.wildlifeType }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hunting_seasons),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightColors.background
                )
            )
        },
        containerColor = NightColors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Bundesland selector
                ExposedDropdownMenuBox(
                    expanded = bundeslandExpanded,
                    onExpandedChange = { bundeslandExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedBundesland.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.bundesland)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bundeslandExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NightColors.primary,
                            unfocusedBorderColor = NightColors.onBackground,
                            focusedLabelColor = NightColors.primary,
                            unfocusedLabelColor = NightColors.onBackground,
                            focusedTextColor = NightColors.onSurface,
                            unfocusedTextColor = NightColors.onSurface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = bundeslandExpanded,
                        onDismissRequest = { bundeslandExpanded = false }
                    ) {
                        HuntingSeasonData.Bundesland.entries.forEach { bl ->
                            DropdownMenuItem(
                                text = { Text(bl.displayName) },
                                onClick = {
                                    selectedBundesland = bl
                                    bundeslandExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Filter toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_active_only),
                        color = NightColors.onSurface,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = showOnlyInSeason,
                        onCheckedChange = { showOnlyInSeason = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NightColors.primary,
                            checkedTrackColor = NightColors.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = NightColors.success
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.hunting_seasons_active, inSeasonSeasons.size),
                                color = NightColors.success,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (upcomingSeasons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = NightColors.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.starting_soon, upcomingSeasons.size),
                                    color = NightColors.primary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Upcoming seasons
            if (upcomingSeasons.isNotEmpty() && !showOnlyInSeason) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSectionHeader(
                        icon = {
                            Icon(
                                Icons.Filled.Upcoming,
                                contentDescription = null,
                                tint = NightColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        title = stringResource(R.string.starting_soon_section)
                    )
                }

                items(upcomingSeasons.take(5)) { (season, daysUntil) ->
                    UpcomingSeasonCard(
                        season = season,
                        daysUntil = daysUntil
                    )
                }
            }

            // Seasons by wildlife type
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader(
                    icon = {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = NightColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    title = if (showOnlyInSeason) {
                        stringResource(R.string.active_hunting_seasons)
                    } else {
                        stringResource(R.string.all_hunting_seasons)
                    }
                )
            }

            groupedSeasons.forEach { (wildlifeType, seasons) ->
                item {
                    WildlifeTypeSection(
                        wildlifeType = wildlifeType,
                        seasons = seasons
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                // Note about federal seasons
                Text(
                    text = stringResource(R.string.hunting_seasons_note),
                    color = NightColors.onBackground,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun WildlifeTypeSection(
    wildlifeType: String,
    seasons: List<HuntingSeasonData.HuntingSeason>
) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH) + 1
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NightColors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = wildlifeType,
                color = NightColors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            seasons.forEach { season ->
                val isInSeason = season.isInSeason(currentMonth, currentDay)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isInSeason) NightColors.success else NightColors.onBackground,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = season.gender ?: stringResource(R.string.all_genders),
                            color = if (isInSeason) NightColors.onSurface else NightColors.onBackground,
                            fontSize = 14.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = season.formatSeasonPeriod(),
                            color = if (isInSeason) NightColors.primary else NightColors.onBackground,
                            fontSize = 12.sp,
                            fontWeight = if (isInSeason) FontWeight.Medium else FontWeight.Normal
                        )
                        if (season.notes != null) {
                            Text(
                                text = season.notes,
                                color = NightColors.error,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingSeasonCard(
    season: HuntingSeasonData.HuntingSeason,
    daysUntil: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NightColors.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${season.wildlifeType} ${season.gender ?: ""}".trim(),
                    color = NightColors.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.starts_on,
                        season.startDay,
                        getMonthName(season.startMonth)
                    ),
                    color = NightColors.onBackground,
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NightColors.primary
            ) {
                Text(
                    text = stringResource(R.string.in_days, daysUntil),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> stringResource(R.string.month_january)
        2 -> stringResource(R.string.month_february)
        3 -> stringResource(R.string.month_march)
        4 -> stringResource(R.string.month_april)
        5 -> stringResource(R.string.month_may)
        6 -> stringResource(R.string.month_june)
        7 -> stringResource(R.string.month_july)
        8 -> stringResource(R.string.month_august)
        9 -> stringResource(R.string.month_september)
        10 -> stringResource(R.string.month_october)
        11 -> stringResource(R.string.month_november)
        12 -> stringResource(R.string.month_december)
        else -> ""
    }
}
