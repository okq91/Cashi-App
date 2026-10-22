package com.cashi.app.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatching] for suspending code: cancellation is rethrown instead of being captured as a
 * failure, so cancelling a job never surfaces as an error to the user.
 */
suspend inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
