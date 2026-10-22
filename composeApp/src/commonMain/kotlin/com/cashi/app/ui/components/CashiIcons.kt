package com.cashi.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The mockup's stroke icons (24x24 viewBox), tinted by the Icon that draws them. */
object CashiIcons {
    val Back: ImageVector by lazy { strokeIcon("Back", 2.2f) { moveTo(15f, 6f); lineTo(9f, 12f); lineTo(15f, 18f) } }

    val Plus: ImageVector by lazy {
        strokeIcon("Plus", 2.4f) {
            moveTo(12f, 5f); verticalLineTo(19f)
            moveTo(5f, 12f); horizontalLineTo(19f)
        }
    }

    val Send: ImageVector by lazy {
        strokeIcon("Send", 2.2f) {
            moveTo(22f, 2f); lineTo(11f, 13f)
            moveTo(22f, 2f); lineTo(15f, 22f); lineTo(11f, 13f); lineTo(2f, 9f); lineTo(22f, 2f); close()
        }
    }

    val ChevronDown: ImageVector by lazy { strokeIcon("ChevronDown", 2.2f) { moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) } }

    private fun strokeIcon(name: String, width: Float, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()
}
