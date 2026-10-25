package com.cashi.app.presentation

import com.cashi.app.domain.error.ApiException
import com.cashi.app.domain.model.Credentials
import com.cashi.app.domain.model.User
import com.cashi.app.domain.repository.AuthRepository
import com.cashi.app.domain.usecase.SignInUseCase
import com.cashi.app.presentation.app.AppIntent
import com.cashi.app.presentation.app.AppViewModel
import com.cashi.app.testing.MainDispatcherExtension
import com.cashi.app.testing.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MainDispatcherExtension::class)
class AppViewModelTest {
    private val credentials = Credentials("demo@cashi.app", "secret")
    private val user = User("u1", "demo@cashi.app", "Demo User")
    private val authRepository = mock<AuthRepository>()

    private fun viewModel() = AppViewModel(SignInUseCase(authRepository, credentials))

    @Test
    fun `signs in silently on launch`() = runTest {
        whenever(authRepository.login(any())).thenReturn(user)

        val viewModel = viewModel()

        with(viewModel.state.value) {
            assertTrue(isSignedIn)
            assertFalse(isSigningIn)
            assertNull(error)
        }
        verify(authRepository).login(credentials)
    }

    @Test
    fun `shows the error and signs in on retry`() = runTest {
        whenever(authRepository.login(any()))
            .thenAnswer { throw ApiException.network() }
            .thenReturn(user)
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.isSignedIn)
        assertEquals("Unable to reach the server. Check your connection and try again.", viewModel.state.value.error)

        viewModel.onIntent(AppIntent.Retry)

        assertTrue(viewModel.state.value.isSignedIn)
        assertNull(viewModel.state.value.error)
        verify(authRepository, times(2)).login(credentials)
    }

    @Test
    fun `retry does nothing once signed in`() = runTest {
        whenever(authRepository.login(any())).thenReturn(user)
        val viewModel = viewModel()

        viewModel.onIntent(AppIntent.Retry)

        verify(authRepository, times(1)).login(credentials)
    }
}
