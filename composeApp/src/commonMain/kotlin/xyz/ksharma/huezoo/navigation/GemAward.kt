package xyz.ksharma.huezoo.navigation

import kotlinx.serialization.Serializable

/**
 * A single gem reward line item — passed from game ViewModels through the
 * Result nav arg so the Result screen can animate each award one by one.
 */
@Serializable
data class GemAward(val label: String, val amount: Int)
