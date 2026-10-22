package com.kanae.foldpage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.kanae.foldpage.BuildInfo
import com.kanae.foldpage.data.AppLanguage
import com.kanae.foldpage.data.AppSettings
import com.kanae.foldpage.data.ThemeMode
import com.kanae.foldpage.ui.i18n.LocalUiStrings
import com.kanae.foldpage.ui.i18n.UiKey
import com.kanae.foldpage.ui.theme.LocalAppDarkTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalUiStrings.current
    val dark = LocalAppDarkTheme.current
    val context = LocalContext.current
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current
    val openDeveloper = {
        runCatching { uriHandler.openUri(BuildInfo.DEVELOPER_URL) }
        Unit
    }

    BackHandler { onBack() }
    SideEffect {
        context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(strings.get(UiKey.SETTINGS_TITLE), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.get(UiKey.BACK_TO_LIBRARY),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SettingsSection(strings.get(UiKey.LANGUAGE))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    AppLanguage.TRADITIONAL to strings.get(UiKey.TRADITIONAL_CHINESE),
                    AppLanguage.SIMPLIFIED to strings.get(UiKey.SIMPLIFIED_CHINESE),
                    AppLanguage.ENGLISH to strings.get(UiKey.ENGLISH),
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = settings.language == option.first,
                        onClick = { onUpdate { it.copy(language = option.first) } },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(option.second, maxLines = 1) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SettingsSection(strings.get(UiKey.THEME))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    ThemeMode.SYSTEM to strings.get(UiKey.THEME_SYSTEM),
                    ThemeMode.LIGHT to strings.get(UiKey.THEME_LIGHT),
                    ThemeMode.DARK to strings.get(UiKey.THEME_DARK),
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = settings.themeMode == option.first,
                        onClick = { onUpdate { it.copy(themeMode = option.first) } },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(option.second, maxLines = 1) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SettingsSection(strings.get(UiKey.DISPLAY_EFFECTS))
            SwitchRow(
                title = strings.get(UiKey.EYE_CARE),
                hint = strings.get(UiKey.EYE_CARE_HINT),
                checked = settings.eyeCareEnabled,
                onCheckedChange = { checked -> onUpdate { current -> current.copy(eyeCareEnabled = checked) } },
            )
            if (settings.eyeCareEnabled) {
                Slider(
                    value = settings.eyeCareStrength,
                    onValueChange = { value -> onUpdate { current -> current.copy(eyeCareStrength = value) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    strings.get(UiKey.STRENGTH, (settings.eyeCareStrength * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            SwitchRow(
                title = strings.get(UiKey.PAPER_GRAIN),
                hint = strings.get(UiKey.PAPER_GRAIN_HINT),
                checked = settings.grainEnabled,
                onCheckedChange = { checked -> onUpdate { current -> current.copy(grainEnabled = checked) } },
            )
            if (settings.grainEnabled) {
                Slider(
                    value = settings.grainStrength,
                    onValueChange = { value -> onUpdate { current -> current.copy(grainStrength = value) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    strings.get(UiKey.STRENGTH, (settings.grainStrength * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                strings.get(UiKey.READER_WHITE_NOTE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            SettingsSection(strings.get(UiKey.ABOUT))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            strings.get(UiKey.APP_NAME),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                "v${BuildInfo.VERSION}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        strings.get(UiKey.ABOUT_SUMMARY),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        strings.get(UiKey.DEVELOPER),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        BuildInfo.DEVELOPER_NAME,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { openDeveloper() },
                    )
                    Text(
                        "github.com/Amamiya-Kanae",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { openDeveloper() },
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
