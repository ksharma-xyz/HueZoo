package xyz.ksharma.huezoo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleSmall
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.settings.state.SettingsUiEvent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onViewHealthNotice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = HuezooColors.AccentCyan,
        secondaryColor = HuezooColors.AccentPurple,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HuezooTopBar(onBackClick = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HuezooSpacing.md)
                    .navigationBarsPadding(),
            ) {
                Spacer(Modifier.height(HuezooSpacing.lg))

                // ── HEALTH ────────────────────────────────────────────────────
                SettingsSectionLabel("HEALTH")
                Spacer(Modifier.height(HuezooSpacing.sm))

                SettingsPanel {
                    SettingsRow(
                        label = "Eye Strain Notice",
                        description = "Review the health guidance shown on first launch.",
                    )
                    Spacer(Modifier.height(HuezooSpacing.sm))
                    HuezooButton(
                        text = "VIEW NOTICE",
                        onClick = onViewHealthNotice,
                        variant = HuezooButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // ── DEBUG (debug builds only) ─────────────────────────────────
                if (state.isDebugBuild) {
                    Spacer(Modifier.height(HuezooSpacing.xl))

                    SettingsSectionLabel("DEBUG")
                    Spacer(Modifier.height(HuezooSpacing.sm))

                    SettingsPanel {
                        SettingsRow(
                            label = "Paid Status",
                            description = if (state.isPaid) "Currently: PAID ✓" else "Currently: FREE",
                        )
                        Spacer(Modifier.height(HuezooSpacing.sm))
                        HuezooButton(
                            text = if (state.isPaid) "SWITCH TO FREE" else "SWITCH TO PAID",
                            onClick = { viewModel.onUiEvent(SettingsUiEvent.TogglePaid) },
                            variant = HuezooButtonVariant.Ghost,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(HuezooSpacing.md))

                    SettingsPanel {
                        SettingsRow(
                            label = "Reset All Data",
                            description = "Wipes sessions, daily records, personal bests, and settings.",
                        )
                        Spacer(Modifier.height(HuezooSpacing.sm))

                        if (state.showResetConfirm) {
                            // Confirmation step
                            HuezooBodyMedium(
                                text = "This cannot be undone. All progress will be lost.",
                                color = HuezooColors.AccentMagenta,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(HuezooSpacing.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                            ) {
                                HuezooButton(
                                    text = "CANCEL",
                                    onClick = { viewModel.onUiEvent(SettingsUiEvent.ResetAllDismissed) },
                                    variant = HuezooButtonVariant.Ghost,
                                    modifier = Modifier.weight(1f),
                                )
                                HuezooButton(
                                    text = "WIPE IT",
                                    onClick = { viewModel.onUiEvent(SettingsUiEvent.ResetAllConfirmed) },
                                    variant = HuezooButtonVariant.Danger,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            HuezooButton(
                                text = "RESET ALL DATA",
                                onClick = { viewModel.onUiEvent(SettingsUiEvent.ResetAllTapped) },
                                variant = HuezooButtonVariant.GhostDanger,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(HuezooSpacing.xl))
            }
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    HuezooLabelSmall(
        text = text,
        modifier = modifier,
        color = HuezooColors.TextDisabled,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HuezooColors.SurfaceL1)
            .padding(HuezooSpacing.md),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HuezooTitleSmall(text = label, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        HuezooBodyMedium(
            text = description,
            color = HuezooColors.TextSecondary,
        )
    }
}

