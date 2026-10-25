package com.cashi.app.ui.screens.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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
import com.cashi.app.ui.theme.CashiTheme
import com.cashi.app.ui.util.CollectEffects
import com.cashi.app.ui.util.TestTags
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentsScreen(onAddPayment: () -> Unit, viewModel: PaymentsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.payments.collectAsLazyPagingItems()
    val snackbar = LocalSnackbarHostState.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PaymentsEffect.OpenAddPayment -> onAddPayment()
        }
    }
    // A failed refresh keeps the list on screen; only an empty list turns into the error state.
    val refresh = items.loadState.refresh
    LaunchedEffect(refresh) {
        if (refresh is LoadState.Error && items.itemCount > 0) snackbar.showSnackbar(refresh.error.message ?: LOAD_ERROR)
    }
    PaymentsContent(state, items, viewModel::onIntent)
}

@Composable
fun PaymentsContent(state: PaymentsState, items: LazyPagingItems<PaymentItem>, onIntent: (PaymentsIntent) -> Unit) {
    val refresh = items.loadState.refresh
    Box(Modifier.fillMaxSize().background(CashiColors.Background).testTag(TestTags.PAYMENTS_SCREEN)) {
        PullToRefreshBox(
            isRefreshing = refresh is LoadState.Loading && items.itemCount > 0,
            onRefresh = {
                onIntent(PaymentsIntent.Refresh)
                items.refresh()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TestTags.HISTORY_LIST),
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                stickyHeader(key = "header") { Header(state.greeting) }
                item(key = "history_title") { HistoryTitle() }
                when {
                    items.itemCount > 0 -> {
                        items(count = items.itemCount, key = items.itemKey { it.id }) { index -> items[index]?.let { PaymentCard(it) } }
                        when (items.loadState.append) {
                            is LoadState.Loading -> item(key = "loading_more") { LoadingMore() }
                            is LoadState.Error -> item(key = "load_more_error") { LoadMoreError(onRetry = items::retry) }
                            is LoadState.NotLoading -> Unit
                        }
                    }
                    refresh is LoadState.Loading -> items(5) { PaymentPlaceholder() }
                    refresh is LoadState.Error -> item(key = "error") { HistoryError(refresh.error.message ?: LOAD_ERROR, onRetry = items::retry) }
                    else -> item(key = "empty") { HistoryEmpty() }
                }
            }
        }
        BottomAction(onClick = { onIntent(PaymentsIntent.AddPaymentClicked) }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private const val LOAD_ERROR = "Could not load payments"

@Composable
private fun Header(greeting: String) {
    Column(
        Modifier.fillMaxWidth().background(CashiColors.Background).statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(greeting, style = CashiText.Greeting)
        Text("Payments", style = CashiText.ScreenTitle)
    }
}

@Composable
private fun HistoryTitle() {
    Text(
        "History",
        style = CashiText.SectionTitle,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 2.dp),
    )
}

@Composable
private fun PaymentCard(item: PaymentItem) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(CashiColors.Surface, shape)
            .border(1.dp, CashiColors.CardBorder, shape)
            .padding(16.dp)
            .testTag(TestTags.PAYMENT_ITEM),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PaymentBadge(item.id)
            Text(
                item.title,
                style = CashiText.Label.copy(fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag(TestTags.PAYMENT_TITLE),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CashiColors.AccentDark)) { append(item.amount) }
                    withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CashiColors.TextMuted)) { append(" ${item.currency}") }
                },
                modifier = Modifier.testTag(TestTags.PAYMENT_AMOUNT),
            )
        }
        Row(
            Modifier.fillMaxWidth().background(CashiColors.RecipientBackground, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(CashiIcons.Mail, contentDescription = null, tint = CashiColors.TextMuted, modifier = Modifier.size(16.dp))
            Text("To", style = CashiText.Label.copy(color = CashiColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            Text(
                item.recipientEmail,
                style = CashiText.Label.copy(color = CashiColors.AccentDark, fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag(TestTags.PAYMENT_EMAIL),
            )
        }
        DashedDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(CashiIcons.Clock, contentDescription = null, tint = CashiColors.TextMuted, modifier = Modifier.size(14.dp))
                Text(item.date, style = CashiText.Helper, modifier = Modifier.testTag(TestTags.PAYMENT_DATE))
            }
            Row(
                Modifier.background(CashiColors.SuccessBackground, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(CashiIcons.Check, contentDescription = null, tint = CashiColors.SuccessText, modifier = Modifier.size(11.dp))
                Text(item.status, style = CashiText.Label.copy(color = CashiColors.SuccessText, fontSize = 12.sp), modifier = Modifier.testTag(TestTags.PAYMENT_STATUS))
            }
        }
    }
}

/** Purple or gold depending on the payment id, so a payment keeps its color as the list changes. */
@Composable
private fun PaymentBadge(id: String) {
    Box(
        Modifier
            .size(44.dp)
            .background( CashiColors.BadgeGoldBackground, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            CashiIcons.Banknote,
            contentDescription = null,
            tint =  CashiColors.BadgeGoldForeground ,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun DashedDivider() {
    val color = CashiColors.InputBorder
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        )
    }
}

@Composable
private fun PaymentPlaceholder() {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(CashiColors.Surface, shape)
            .border(1.dp, CashiColors.CardBorder, shape)
            .padding(16.dp)
            .testTag(TestTags.HISTORY_LOADING),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBar(Modifier.size(44.dp))
            SkeletonBar(Modifier.weight(1f).height(14.dp))
            SkeletonBar(Modifier.width(72.dp).height(14.dp))
        }
        SkeletonBar(Modifier.fillMaxWidth().height(36.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SkeletonBar(Modifier.width(110.dp).height(12.dp))
            SkeletonBar(Modifier.width(56.dp).height(20.dp))
        }
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
private fun HistoryError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = CashiText.Body, textAlign = TextAlign.Center, modifier = Modifier.testTag(TestTags.HISTORY_ERROR))
        Text(
            "Try again",
            style = CashiText.Link,
            modifier = Modifier.clickable(role = Role.Button, onClick = onRetry).padding(8.dp).testTag(TestTags.HISTORY_RETRY),
        )
    }
}

@Composable
private fun LoadingMore() {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), color = CashiColors.Accent, strokeWidth = 2.5.dp)
    }
}

@Composable
private fun LoadMoreError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Couldn't load more payments", style = CashiText.Body)
        Text(
            "Try again",
            style = CashiText.Link,
            modifier = Modifier.clickable(role = Role.Button, onClick = onRetry).padding(8.dp).testTag(TestTags.HISTORY_LOAD_MORE_RETRY),
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

private val previewPayments = listOf(
    PaymentItem("1", "ahmad@gmail.com", "ahmad@gmail.com", "Today, 9:41 AM", "320.00", "USD"),
    PaymentItem("2", "Rent", "sara@example.com", "Oct 20, 2:15 PM", "450.00", "JOD"),
    PaymentItem("3", "Loan", "omar@example.com", "Oct 12, 11:02 AM", "1,500.50", "EGP"),
)

private val previewState = PaymentsState(greeting = "Good morning, Omar")

private val idle = LoadState.NotLoading(endOfPaginationReached = true)

@Composable
private fun previewItems(
    items: List<PaymentItem>,
    refresh: LoadState = idle,
    append: LoadState = idle,
): LazyPagingItems<PaymentItem> {
    val data = remember(items, refresh, append) {
        flowOf(PagingData.from(items, sourceLoadStates = LoadStates(refresh = refresh, prepend = idle, append = append)))
    }
    return data.collectAsLazyPagingItems()
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PaymentsContentPreview() = CashiTheme { PaymentsContent(previewState, previewItems(previewPayments), onIntent = {}) }

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PaymentsLoadingMorePreview() = CashiTheme {
    PaymentsContent(previewState, previewItems(previewPayments, append = LoadState.Loading), onIntent = {})
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PaymentsLoadMoreErrorPreview() = CashiTheme {
    PaymentsContent(previewState, previewItems(previewPayments, append = LoadState.Error(RuntimeException())), onIntent = {})
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PaymentsLoadingPreview() = CashiTheme {
    PaymentsContent(PaymentsState(greeting = "Good morning, Omar"), previewItems(emptyList(), refresh = LoadState.Loading), onIntent = {})
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PaymentsEmptyPreview() = CashiTheme {
    PaymentsContent(previewState, previewItems(emptyList()), onIntent = {})
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun PaymentsErrorPreview() = CashiTheme {
    val error = LoadState.Error(RuntimeException("Unable to reach the server. Check your connection and try again."))
    PaymentsContent(previewState, previewItems(emptyList(), refresh = error), onIntent = {})
}

@Preview(showBackground = true)
@Composable
private fun HeaderPreview() = CashiTheme { Header("Good morning, Omar") }

@Preview(showBackground = true)
@Composable
private fun HistoryTitlePreview() = CashiTheme { HistoryTitle() }

@Preview(showBackground = true)
@Composable
private fun PaymentRowPreview() = CashiTheme { PaymentCard(previewPayments.first()) }

@Preview(showBackground = true)
@Composable
private fun PaymentPlaceholderPreview() = CashiTheme { PaymentPlaceholder() }

@Preview(showBackground = true)
@Composable
private fun HistoryEmptyPreview() = CashiTheme { HistoryEmpty() }

@Preview(showBackground = true)
@Composable
private fun HistoryErrorPreview() = CashiTheme { HistoryError("Couldn't load payments", onRetry = {}) }

@Preview(showBackground = true)
@Composable
private fun LoadingMorePreview() = CashiTheme { LoadingMore() }

@Preview(showBackground = true)
@Composable
private fun LoadMoreErrorPreview() = CashiTheme { LoadMoreError(onRetry = {}) }

@Preview(showBackground = true)
@Composable
private fun BottomActionPreview() = CashiTheme { BottomAction(onClick = {}, modifier = Modifier) }
