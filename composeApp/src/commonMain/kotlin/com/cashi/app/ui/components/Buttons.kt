package com.cashi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cashi.app.ui.theme.CashiColors
import com.cashi.app.ui.theme.CashiText

/** The mockup's yellow 56dp call-to-action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(CashiColors.Yellow, shape)
            .clickable(enabled = enabled && !isLoading, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = CashiColors.OnYellow, strokeWidth = 2.5.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.let { Icon(it, contentDescription = null, tint = CashiColors.OnYellow, modifier = Modifier.size(iconSize)) }
                Text(text, style = CashiText.Button)
                trailingIcon?.let { Icon(it, contentDescription = null, tint = CashiColors.OnYellow, modifier = Modifier.size(iconSize)) }
            }
        }
    }
}

/** 44dp white square with a chevron, used as the top bar's back button. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(shape)
            .background(CashiColors.Surface, shape)
            .border(1.dp, CashiColors.BackButtonBorder, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(CashiIcons.Back, contentDescription = "Back to payments", tint = CashiColors.TextPrimary, modifier = Modifier.size(20.dp))
    }
}
