package com.majidbahmani.cesto.di

import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingViewModel
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame

/** Koin resolves at runtime: a missing binding only shows up here, not at compile time. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModulesTest {

    private object FakeGmailAuthorizer : GmailAuthorizer {
        override fun authorize(interactive: Boolean, onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) =
            onFailure(GmailAuthError.NOT_GRANTED)
    }

    // Local KoinApplication: tests never touch the global Koin instance.
    private val app = koinApplication { modules(listOf(platformServicesModule(FakeGmailAuthorizer)) + appModules) }

    // ViewModels launch in viewModelScope (Dispatchers.Main), which doesn't exist in JVM tests.
    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() {
        app.close()
        Dispatchers.resetMain()
    }

    @Test
    fun httpClient_isSingleton() {
        assertSame(app.koin.get<HttpClient>(), app.koin.get<HttpClient>())
    }

    @Test
    fun httpEngine_isResolvedForThisPlatform() {
        app.koin.get<HttpClientEngine>()
    }

    @Test
    fun gmailAuthorizer_isThePlatformInstance() {
        assertSame<GmailAuthorizer>(FakeGmailAuthorizer, app.koin.get())
    }

    @Test
    fun onboardingViewModel_resolves() {
        app.koin.get<OnboardingViewModel>()
    }
}
