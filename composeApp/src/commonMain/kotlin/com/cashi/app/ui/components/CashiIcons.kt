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

    val Banknote: ImageVector by lazy {
        strokeIcon("Banknote", 1.8f) {
            moveTo(4f, 6f); horizontalLineTo(20f); arcTo(2f, 2f, 0f, false, true, 22f, 8f); verticalLineTo(16f)
            arcTo(2f, 2f, 0f, false, true, 20f, 18f); horizontalLineTo(4f); arcTo(2f, 2f, 0f, false, true, 2f, 16f)
            verticalLineTo(8f); arcTo(2f, 2f, 0f, false, true, 4f, 6f); close()
            moveTo(9.5f, 12f); arcToRelative(2.5f, 2.5f, 0f, true, true, 5f, 0f); arcToRelative(2.5f, 2.5f, 0f, true, true, -5f, 0f)
            moveTo(6f, 12f); lineTo(6.01f, 12f)
            moveTo(18f, 12f); lineTo(18.01f, 12f)
        }
    }

    val Mail: ImageVector by lazy {
        strokeIcon("Mail", 1.8f) {
            moveTo(5f, 5f); horizontalLineTo(19f); arcTo(2f, 2f, 0f, false, true, 21f, 7f); verticalLineTo(17f)
            arcTo(2f, 2f, 0f, false, true, 19f, 19f); horizontalLineTo(5f); arcTo(2f, 2f, 0f, false, true, 3f, 17f)
            verticalLineTo(7f); arcTo(2f, 2f, 0f, false, true, 5f, 5f); close()
            moveTo(3.5f, 7.5f); lineTo(12f, 13f); lineTo(20.5f, 7.5f)
        }
    }

    val Clock: ImageVector by lazy {
        strokeIcon("Clock", 1.8f) {
            moveTo(3f, 12f); arcToRelative(9f, 9f, 0f, true, true, 18f, 0f); arcToRelative(9f, 9f, 0f, true, true, -18f, 0f)
            moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15f, 14f)
        }
    }

    val Check: ImageVector by lazy { strokeIcon("Check", 2.6f) { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7.5f) } }

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
