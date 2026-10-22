package com.cashi.app.ui.screens.addpayment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.presentation.addpayment.AddPaymentEffect
import com.cashi.app.presentation.addpayment.AddPaymentIntent
import com.cashi.app.presentation.addpayment.AddPaymentState
import com.cashi.app.presentation.addpayment.AddPaymentViewModel
import com.cashi.app.ui.LocalSnackbarHostState
import com.cashi.app.ui.components.BackButton
import com.cashi.app.ui.components.CashiIcons
import com.cashi.app.ui.components.CashiTextField
import com.cashi.app.ui.components.LabeledField
import com.cashi.app.ui.components.PrimaryButton
import com.cashi.app.ui.components.inputFrame
import com.cashi.app.ui.theme.CashiColors
import com.cashi.app.ui.theme.CashiText
import com.cashi.app.ui.util.CollectEffects
import com.cashi.app.ui.util.TestTags
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddPaymentScreen(onBack: () -> Unit, viewModel: AddPaymentViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    // The snackbar must outlive this screen, so it is shown from a scope that survives navigation.
    val appScope = rememberCoroutineScope()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is AddPaymentEffect.PaymentSent -> {
                onBack()
                appScope.launch { snackbar.showSnackbar(effect.message) }
            }
            is AddPaymentEffect.ShowError -> snackbar.showSnackbar(effect.message)
            AddPaymentEffect.NavigateBack -> onBack()
        }
    }
    AddPaymentContent(state, viewModel::onIntent)
}

@Composable
fun AddPaymentContent(state: AddPaymentState, onIntent: (AddPaymentIntent) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .background(CashiColors.Background)
            .imePadding()
            .testTag(TestTags.ADD_PAYMENT_SCREEN),
    ) {
        Row(
            Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = { onIntent(AddPaymentIntent.Back) }, modifier = Modifier.testTag(TestTags.BACK_BUTTON))
            Text(
                "New payment",
                style = CashiText.TopBarTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(end = 44.dp),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val emailError = state.errors[PaymentField.RECIPIENT_EMAIL]
            LabeledField("Recipient email", emailError, TestTags.RECIPIENT_EMAIL_ERROR) {
                CashiTextField(
                    value = state.recipientEmail,
                    onValueChange = { onIntent(AddPaymentIntent.RecipientEmailChanged(it)) },
                    placeholder = "name@example.com",
                    testTag = TestTags.RECIPIENT_EMAIL_INPUT,
                    isError = emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
            }

            AmountCard(state, onIntent, onNext = { focusManager.moveFocus(FocusDirection.Down) })

            val currencyError = state.errors[PaymentField.CURRENCY]
            LabeledField("Currency", currencyError, TestTags.CURRENCY_ERROR) {
                CurrencyPicker(state.currency, state.currencies, isError = currencyError != null) {
                    onIntent(AddPaymentIntent.CurrencySelected(it))
                }
            }

            val descriptionError = state.errors[PaymentField.DESCRIPTION]
            LabeledField("Description", descriptionError, TestTags.DESCRIPTION_ERROR) {
                CashiTextField(
                    value = state.description,
                    onValueChange = { onIntent(AddPaymentIntent.DescriptionChanged(it)) },
                    placeholder = "What is this payment for?",
                    testTag = TestTags.DESCRIPTION_INPUT,
                    isError = descriptionError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
        }

        PrimaryButton(
            text = "Send",
            onClick = {
                focusManager.clearFocus()
                onIntent(AddPaymentIntent.Send)
            },
            trailingIcon = CashiIcons.Send,
            iconSize = 18.dp,
            isLoading = state.isSending,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
                .testTag(TestTags.SEND_BUTTON),
        )
    }
}

@Composable
private fun AmountCard(state: AddPaymentState, onIntent: (AddPaymentIntent) -> Unit, onNext: () -> Unit) {
    val error = state.errors[PaymentField.AMOUNT]
    val shape = RoundedCornerShape(22.dp)
    val amountStyle = CashiText.ScreenTitle.copy(fontSize = 44.sp, letterSpacing = (-1).sp, textAlign = TextAlign.Center)
    Column(
        Modifier
            .fillMaxWidth()
            .background(CashiColors.Surface, shape)
            .border(1.dp, if (error != null) CashiColors.Error else CashiColors.CardBorder, shape)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Amount", style = CashiText.Label.copy(color = CashiColors.TextMuted, fontSize = 13.sp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            BasicTextField(
                value = state.amount,
                onValueChange = { onIntent(AddPaymentIntent.AmountChanged(it)) },
                singleLine = true,
                textStyle = amountStyle,
                cursorBrush = SolidColor(CashiColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 48.dp, max = 220.dp).testTag(TestTags.AMOUNT_INPUT),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (state.amount.isEmpty()) Text("0.00", style = amountStyle.copy(color = CashiColors.Placeholder))
                        inner()
                    }
                },
            )
            Text(state.currency.code, style = CashiText.Label.copy(color = CashiColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
        }
        if (error != null) {
            Text(error, style = CashiText.ErrorText, modifier = Modifier.testTag(TestTags.AMOUNT_ERROR))
        } else {
            Text("Up to 2 decimal places", style = CashiText.Helper)
        }
    }
}

@Composable
private fun CurrencyPicker(
    selected: Currency,
    options: List<Currency>,
    isError: Boolean,
    onSelect: (Currency) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .inputFrame(isFocused = expanded, isError = isError)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 16.dp, end = 16.dp)
                .testTag(TestTags.CURRENCY_DROPDOWN),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label(selected), style = CashiText.Input.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
            Icon(CashiIcons.ChevronDown, contentDescription = null, tint = CashiColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = CashiColors.Surface,
        ) {
            options.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(label(currency), style = CashiText.Input) },
                    onClick = {
                        expanded = false
                        onSelect(currency)
                    },
                    modifier = Modifier.testTag(TestTags.currencyOption(currency.code)),
                )
            }
        }
    }
}

private fun label(currency: Currency) = "${currency.code} — ${currency.displayName}"
