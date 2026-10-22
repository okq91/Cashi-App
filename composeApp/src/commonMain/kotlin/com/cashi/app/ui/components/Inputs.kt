package com.cashi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cashi.app.ui.theme.CashiColors
import com.cashi.app.ui.theme.CashiText

/** Label above a control, with the field's validation message below it. */
@Composable
fun LabeledField(
    label: String,
    error: String?,
    errorTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = CashiText.Label)
        content()
        if (error != null) Text(error, style = CashiText.ErrorText, modifier = Modifier.testTag(errorTag))
    }
}

/** Outline of a 56dp mockup input: grey border, accent border + soft glow when focused, red on error. */
fun Modifier.inputFrame(isFocused: Boolean, isError: Boolean): Modifier {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = when {
        isError -> CashiColors.Error
        isFocused -> CashiColors.Accent
        else -> CashiColors.InputBorder
    }
    return this
        .then(if (isFocused) Modifier.shadow(3.dp, shape, ambientColor = CashiColors.Accent, spotColor = CashiColors.Accent) else Modifier)
        .fillMaxWidth()
        .height(56.dp)
        .background(CashiColors.Surface, shape)
        .border(1.dp, borderColor, shape)
}

@Composable
fun CashiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = CashiText.Input,
        cursorBrush = SolidColor(CashiColors.Accent),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        modifier = modifier
            .inputFrame(isFocused, isError)
            .testTag(testTag),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = CashiText.Input.copy(color = CashiColors.Placeholder))
                inner()
            }
        },
    )
}
