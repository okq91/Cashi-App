package com.cashi.app.testing

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.Flow

/** A single-page [PagingSource] over a fixed list, for ViewModel tests that mock the history use case. */
private class ListPagingSource<T : Any>(private val items: List<T>) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = LoadResult.Page(items, prevKey = null, nextKey = null)
}

fun <T : Any> pagedOf(items: List<T>): Flow<PagingData<T>> =
    Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) { ListPagingSource(items) }.flow
