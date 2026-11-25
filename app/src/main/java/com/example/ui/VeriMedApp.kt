package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.ThemeMode
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.ProtocolSettingsScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.VerifyScreen
import com.example.ui.theme.LocalClinicalColors
import com.example.ui.theme.VeriMedTheme

const val ROUTE_VERIFY = "verify"
const val ROUTE_AUDIT_LOG = "audit_log"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryDestinations = listOf(
    NavDestinationItem(ROUTE_VERIFY, R.string.nav_verify, Icons.Filled.Shield, Icons.Outlined.Shield),
    NavDestinationItem(ROUTE_AUDIT_LOG, R.string.nav_audit_log, Icons.Filled.History, Icons.Outlined.History),
    NavDestinationItem(ROUTE_SETTINGS, R.string.nav_protocol, Icons.Filled.Tune, Icons.Outlined.Tune)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeriMedApp(
    viewModel: VeriMedViewModel
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val defaultSpecialty by viewModel.defaultSpecialty.collectAsStateWithLifecycle()
    val allAuditReports by viewModel.allAuditReports.collectAsStateWithLifecycle()
    val filteredAuditReports by viewModel.filteredAuditReports.collectAsStateWithLifecycle()
    val selectedModality by viewModel.selectedModality.collectAsStateWithLifecycle()
    val selectedSpecialty by viewModel.selectedSpecialty.collectAsStateWithLifecycle()
    val caseReference by viewModel.caseReference.collectAsStateWithLifecycle()
    val clinicalText by viewModel.clinicalText.collectAsStateWithLifecycle()
    val imageAttachment by viewModel.imageAttachment.collectAsStateWithLifecycle()
    val videoAttachment by viewModel.videoAttachment.collectAsStateWithLifecycle()
    val audioAttachment by viewModel.audioAttachment.collectAsStateWithLifecycle()
    val audioRecorderState by viewModel.audioRecorderState.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val activeReport by viewModel.activeReport.collectAsStateWithLifecycle()
    val auditSearchQuery by viewModel.auditSearchQuery.collectAsStateWithLifecycle()
    val auditModalityFilter by viewModel.auditModalityFilter.collectAsStateWithLifecycle()
    val auditVerdictFilter by viewModel.auditVerdictFilter.collectAsStateWithLifecycle()

    val isDarkResolved = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    VeriMedTheme(themeMode = themeMode) {
        val currentReport = activeReport
        if (currentReport != null) {
            ReportDetailScreen(
                report = currentReport,
                shareableText = viewModel.formatShareableSummary(currentReport),
                onBack = viewModel::closeActiveReport,
                onNewVerification = viewModel::startNewVerification
            )
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_VERIFY

            val navigateToTab: (String) -> Unit = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp
                val colorScheme = MaterialTheme.colorScheme
                val clinical = LocalClinicalColors.current

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocalHospital,
                                                contentDescription = null,
                                                tint = colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.app_subtitle),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.cycleQuickDarkMode(isDarkResolved) },
                                    modifier = Modifier.testTag("quick_theme_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkResolved) {
                                            Icons.Outlined.LightMode
                                        } else {
                                            Icons.Outlined.DarkMode
                                        },
                                        contentDescription = stringResource(R.string.cd_toggle_theme),
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                primaryDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(dest.route) },
                                        modifier = Modifier.testTag("nav_${dest.route}"),
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(dest.labelRes)) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(
                                containerColor = colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(colorScheme.surface)
                            ) {
                                primaryDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTab(dest.route) },
                                        modifier = Modifier.testTag("rail_${dest.route}"),
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(dest.labelRes)) }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(clinical.hairlineBorder)
                                    .padding(end = 1.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (useNavigationRail) Modifier.navigationBarsPadding() else Modifier
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_VERIFY,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 820.dp)
                            ) {
                                composable(ROUTE_VERIFY) {
                                    VerifyScreen(
                                        totalAudits = allAuditReports.size,
                                        selectedModality = selectedModality,
                                        selectedSpecialty = selectedSpecialty,
                                        caseReference = caseReference,
                                        clinicalText = clinicalText,
                                        imageAttachment = imageAttachment,
                                        videoAttachment = videoAttachment,
                                        audioAttachment = audioAttachment,
                                        audioRecorderState = audioRecorderState,
                                        analysisState = analysisState,
                                        canAnalyze = viewModel.canRunAnalysis(),
                                        onSelectModality = viewModel::selectModality,
                                        onSelectSpecialty = viewModel::selectSpecialty,
                                        onCaseRefChange = viewModel::updateCaseReference,
                                        onClinicalTextChange = viewModel::updateClinicalText,
                                        onLoadSampleNote = viewModel::loadSampleClinicalNote,
                                        onImageSelected = viewModel::onImageSelected,
                                        onImageCaptured = viewModel::onImageCaptured,
                                        onClearImage = viewModel::clearImageAttachment,
                                        onVideoSelected = viewModel::onVideoSelected,
                                        onClearVideo = viewModel::clearVideoAttachment,
                                        onAudioSelected = viewModel::onAudioSelected,
                                        onClearAudio = viewModel::clearAudioAttachment,
                                        onStartRecording = viewModel::startAudioRecording,
                                        onStopRecording = viewModel::stopAudioRecording,
                                        onToggleAudioPlayback = viewModel::toggleAudioPlayback,
                                        onRunAnalysis = viewModel::runForensicAnalysis,
                                        onDismissError = viewModel::dismissError
                                    )
                                }

                                composable(ROUTE_AUDIT_LOG) {
                                    AuditLogScreen(
                                        allReports = allAuditReports,
                                        filteredReports = filteredAuditReports,
                                        searchQuery = auditSearchQuery,
                                        modalityFilter = auditModalityFilter,
                                        verdictFilter = auditVerdictFilter,
                                        onSearchChange = viewModel::updateAuditSearch,
                                        onModalityFilterToggle = viewModel::toggleAuditModalityFilter,
                                        onVerdictFilterToggle = viewModel::toggleAuditVerdictFilter,
                                        onOpenReport = viewModel::openReport,
                                        onDeleteReport = viewModel::deleteAuditReport,
                                        onClearAll = viewModel::clearAllAuditReports
                                    )
                                }

                                composable(ROUTE_SETTINGS) {
                                    ProtocolSettingsScreen(
                                        themeMode = themeMode,
                                        defaultSpecialty = defaultSpecialty,
                                        onSelectThemeMode = viewModel::setThemeMode,
                                        onSelectDefaultSpecialty = viewModel::setDefaultSpecialty
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
