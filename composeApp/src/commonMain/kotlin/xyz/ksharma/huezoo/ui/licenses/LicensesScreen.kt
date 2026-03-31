package xyz.ksharma.huezoo.ui.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleSmall
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

private data class LicenseEntry(
    val name: String,
    val author: String,
    val license: String,
    val note: String = "",
)

private val licenses = listOf(
    LicenseEntry(
        name = "Clash Display",
        author = "Indian Type Foundry via Fontshare",
        license = "Fontshare Free Font License",
        note = "Review bundling terms at fontshare.com before App Store submission.",
    ),
    LicenseEntry(
        name = "Bebas Neue",
        author = "Ryoichi Tsunekawa",
        license = "SIL Open Font License 1.1",
    ),
    LicenseEntry(
        name = "Space Grotesk",
        author = "Florian Karsten",
        license = "SIL Open Font License 1.1",
    ),
    LicenseEntry(
        name = "Compose Multiplatform",
        author = "JetBrains s.r.o.",
        license = "Apache License 2.0",
    ),
    LicenseEntry(
        name = "Kotlin",
        author = "JetBrains s.r.o.",
        license = "Apache License 2.0",
    ),
    LicenseEntry(
        name = "Koin",
        author = "insert-koin.io",
        license = "Apache License 2.0",
    ),
    LicenseEntry(
        name = "SQLDelight",
        author = "Cash App",
        license = "Apache License 2.0",
    ),
    LicenseEntry(
        name = "basic-ads (LexiLabs)",
        author = "LexiLabs",
        license = "Apache License 2.0",
    ),
    LicenseEntry(
        name = "Google Mobile Ads SDK",
        author = "Google LLC",
        license = "Google Mobile Ads SDK Terms of Service",
    ),
    LicenseEntry(
        name = "Google Play Billing Library",
        author = "Google LLC",
        license = "Android Software Development Kit License",
    ),
)

/**
 * Displays font and library licenses used in Huezoo.
 *
 * Accessible from Settings → ABOUT → OPEN LICENSES.
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = HuezooColors.AccentPurple,
        secondaryColor = HuezooColors.AccentCyan,
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

                HuezooLabelSmall(
                    text = "OPEN LICENSES",
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(HuezooSpacing.sm))

                licenses.forEachIndexed { index, entry ->
                    if (index > 0) Spacer(Modifier.height(HuezooSpacing.sm))
                    LicenseCard(entry)
                }

                Spacer(Modifier.height(HuezooSpacing.xxl))
            }
        }
    }
}

@Composable
private fun LicenseCard(entry: LicenseEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HuezooColors.SurfaceL1)
            .padding(HuezooSpacing.md),
    ) {
        HuezooTitleSmall(text = entry.name, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        HuezooBodyMedium(text = entry.author, color = HuezooColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        HuezooLabelSmall(text = entry.license, color = HuezooColors.AccentCyan)
        if (entry.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            HuezooLabelSmall(text = entry.note, color = HuezooColors.TextDisabled)
        }
    }
}
