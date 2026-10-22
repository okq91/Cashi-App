package com.cashi.app.ui.screens.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cashi.app.presentation.payments.MonthSummary
import com.cashi.app.presentation.payments.PaymentItem
import com.cashi.app.presentation.payments.PaymentsEffect
import com.cashi.app.presentation.payments.PaymentsIntent
import com.cashi.app.presentation.payments.PaymentsState
import com.cashi.app.presentation.payments.PaymentsViewModel
import com.cashi.app.ui.LocalSnackbarHostState
import com.cashi.app.ui.components.CashiIcons
import com.cashi.app.ui.components.PrimaryButton
import com.cashi.app.ui.theme.CashiColors
import com.cashi.app.ui.theme.CashiText
import com.cashi.app.ui.util.CollectEffects
import com.cashi.app.ui.util.TestTags
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentsScreen(onAddPayment: () -> Unit, viewModel: PaymentsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PaymentsEffect.OpenAddPayment -> onAddPayment()
            is PaymentsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
        }
    }
    PaymentsContent(state, viewModel::onIntent)
}

@Composable
fun PaymentsContent(state: PaymentsState, onIntent: (PaymentsIntent) -> Unit) {
    Box(Modifier.fillMaxSize().background(CashiColors.Background).testTag(TestTags.PAYMENTS_SCREEN)) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(PaymentsIntent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TestTags.HISTORY_LIST),
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") { Header(state.greeting) }
                item(key = "summary") { SummaryCard(state.summary) }
                item(key = "history_title") { HistoryTitle(state, onIntent) }
                val error = state.error
                when {
                    state.isLoading -> items(5) { PaymentPlaceholder() }
                    error != null -> item(key = "error") { HistoryError(error, onIntent) }
                    state.isEmpty -> item(key = "empty") { HistoryEmpty() }
                    else -> items(state.visiblePayments, key = { it.id }) { PaymentRow(it) }
                }
            }
        }
        BottomAction(onClick = { onIntent(PaymentsIntent.AddPaymentClicked) }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Header(greeting: String) {
    Column(
        Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(greeting, style = CashiText.Greeting)
        Text("Payments", style = CashiText.ScreenTitle)
    }
}

@Composable
private fun SummaryCard(summary: MonthSummary?) {
    val white = Color.White
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(CashiColors.Accent, RoundedCornerShape(22.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Sent this month", style = CashiText.Label.copy(color = white.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            Text(
                summary?.month.orEmpty(),
                style = CashiText.Label.copy(color = CashiColors.OnYellow, fontSize = 12.sp),
                modifier = Modifier.background(CashiColors.Yellow, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                summary?.total ?: "0.00",
                style = CashiText.ScreenTitle.copy(color = white, fontSize = 36.sp, letterSpacing = (-1).sp),
                modifier = Modifier.testTag(TestTags.SUMMARY_TOTAL).alignByBaseline(),
            )
            Text(
                summary?.totalCurrency ?: "JOD",
                style = CashiText.Label.copy(color = white.copy(alpha = 0.85f), fontSize = 16.sp),
                modifier = Modifier.alignByBaseline(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat("Payments", (summary?.count ?: 0).toString(), TestTags.SUMMARY_COUNT, Modifier.weight(1f))
            SummaryStat("In USD", summary?.usdTotal ?: "$0.00", TestTags.SUMMARY_USD, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, tag: String, modifier: Modifier) {
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = CashiText.Label.copy(color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
        Text(value, style = CashiText.Label.copy(color = Color.White, fontSize = 16.sp), modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun HistoryTitle(state: PaymentsState, onIntent: (PaymentsIntent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("History", style = CashiText.SectionTitle)
        if (state.canExpand) {
            Text(
                if (state.showAll) "Show less" else "See all",
                style = CashiText.Link,
                modifier = Modifier
                    .clickable(role = Role.Button) { onIntent(PaymentsIntent.ToggleShowAll) }
                    .testTag(TestTags.SEE_ALL),
            )
        }
    }
}

@Composable
private fun PaymentRow(item: PaymentItem) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(CashiColors.Surface, shape)
            .border(1.dp, CashiColors.CardBorder, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(TestTags.PAYMENT_ITEM),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.title,
                style = CashiText.Label.copy(fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(TestTags.PAYMENT_TITLE),
            )
            Text(
                item.recipientEmail,
                style = CashiText.Label.copy(color = CashiColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(TestTags.PAYMENT_EMAIL),
            )
            Text(item.date, style = CashiText.Helper, modifier = Modifier.testTag(TestTags.PAYMENT_DATE))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${item.amount} ${item.currency}",
                style = CashiText.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.testTag(TestTags.PAYMENT_AMOUNT),
            )
            Text(
                item.status,
                style = CashiText.Label.copy(color = CashiColors.SuccessText, fontSize = 11.sp),
                modifier = Modifier
                    .background(CashiColors.SuccessBackground, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag(TestTags.PAYMENT_STATUS),
            )
        }
    }
}

@Composable
private fun PaymentPlaceholder() {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(CashiColors.Surface, shape)
            .border(1.dp, CashiColors.CardBorder, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(TestTags.HISTORY_LOADING),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBar(Modifier.width(140.dp).height(14.dp))
            SkeletonBar(Modifier.width(180.dp).height(12.dp))
            SkeletonBar(Modifier.width(90.dp).height(10.dp))
        }
        SkeletonBar(Modifier.width(72.dp).height(14.dp))
    }
}

@Composable
private fun SkeletonBar(modifier: Modifier) {
    Spacer(modifier.background(CashiColors.Skeleton, RoundedCornerShape(6.dp)))
}

@Composable
private fun HistoryEmpty() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp).testTag(TestTags.HISTORY_EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("No payments yet", style = CashiText.SectionTitle.copy(fontSize = 16.sp))
        Text("Tap \"Add new payment\" to send your first one.", style = CashiText.Body, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HistoryError(message: String, onIntent: (PaymentsIntent) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = CashiText.Body, textAlign = TextAlign.Center, modifier = Modifier.testTag(TestTags.HISTORY_ERROR))
        Text(
            "Try again",
            style = CashiText.Link,
            modifier = Modifier.clickable(role = Role.Button) { onIntent(PaymentsIntent.Retry) }.padding(8.dp).testTag(TestTags.HISTORY_RETRY),
        )
    }
}

@Composable
private fun BottomAction(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to CashiColors.Background.copy(alpha = 0f),
                    0.35f to CashiColors.Background,
                ),
            )
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
    ) {
        PrimaryButton(
            text = "Add new payment",
            onClick = onClick,
            leadingIcon = CashiIcons.Plus,
            modifier = Modifier.testTag(TestTags.ADD_PAYMENT_BUTTON),
        )
    }
}
