package xyz.ksharma.huezoo.domain.color

/**
 * CIE L*a*b* (CIELAB) color representation under the D65 illuminant.
 *
 * CIELAB is a perceptually uniform color space — equal numerical distances
 * correspond roughly to equal perceived color differences.
 * This makes it ideal for computing how "different" two colors look.
 *
 * @property l Lightness: 0 (black) → 100 (white)
 * @property a Red/Green axis: negative = green, positive = red. Roughly −128 to +127.
 * @property b Yellow/Blue axis: negative = blue, positive = yellow. Roughly −128 to +127.
 */
data class Lab(
    val l: Float,
    val a: Float,
    val b: Float,
)
