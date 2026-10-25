package com.cashi.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cashi.app.data.mapper.toDomain
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.domain.model.Payment
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cursor-based history paging: the key is the server's `nextCursor` (null for the first page),
 * and the server decides the page size. Pages stay in the server's newest-first order.
 */
class PaymentsPagingSource(private val api: PaymentsApi) : PagingSource<String, Payment>() {

    // A refresh always restarts from the newest payments.
    override fun getRefreshKey(state: PagingState<String, Payment>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Payment> = try {
        val page = api.getPayments(params.key)
        val items = page.items.map { it.toDomain() }
        LoadResult.Page(data = items, prevKey = null, nextKey = page.nextCursor?.takeIf { it.isNotBlank() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadResult.Error(e)
    }
}
