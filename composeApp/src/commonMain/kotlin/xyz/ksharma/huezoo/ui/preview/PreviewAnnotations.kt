package xyz.ksharma.huezoo.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview for a Huezoo UI component.
 * Shows phone at 1× and 2× font scale (dark-only app — no light mode variant).
 */
@Preview(
    name = "1. Phone",
    group = "Component",
    showBackground = true,
    device = Devices.PHONE,
    backgroundColor = 0xFF080810,
)
@Preview(
    name = "2. 2× Font Scale",
    group = "Component",
    fontScale = 2.0f,
    showBackground = true,
    device = Devices.PHONE,
    backgroundColor = 0xFF080810,
)
annotation class PreviewComponent

/**
 * Multi-preview for a full Huezoo screen.
 * Shows phone + tablet (dark-only app).
 */
@Preview(
    name = "1. Phone",
    group = "Screen",
    showBackground = true,
    device = Devices.PHONE,
    backgroundColor = 0xFF080810,
)
@Preview(
    name = "2. Tablet",
    group = "Screen",
    showBackground = true,
    device = Devices.TABLET,
    backgroundColor = 0xFF080810,
)
annotation class PreviewScreen
