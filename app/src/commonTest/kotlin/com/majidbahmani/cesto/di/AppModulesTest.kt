package com.majidbahmani.cesto.di

import app.cash.sqldelight.db.SqlDriver
import com.majidbahmani.cesto.account.AccountRepository
import com.majidbahmani.cesto.account.SessionStore
import com.majidbahmani.cesto.account.Sessions
import com.majidbahmani.cesto.feature.chat.presentation.viewmodel.ChatViewModel
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingViewModel
import com.majidbahmani.cesto.feature.receipts.data.local.PdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailApi
import com.majidbahmani.cesto.feature.receipts.data.remote.KtorGmailApi
import com.majidbahmani.cesto.feature.receipts.presentation.viewmodel.ReceiptsViewModel
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.SettingsViewModel
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider
import com.majidbahmani.cesto.gmailauth.GoogleSignInError
import com.majidbahmani.cesto.llm.GeminiKeyStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Koin resolves at runtime: a missing binding only shows up here, not at compile time. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModulesTest {

    private object FakeGmailAuthorizer : GmailAuthorizer {
        override fun authorize(interactive: Boolean, onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) =
            onFailure(GmailAuthError.NOT_GRANTED)
    }

    private object FakeGoogleIdTokenProvider : GoogleIdTokenProvider {
        override fun signIn(onSuccess: (String) -> Unit, onFailure: (GoogleSignInError) -> Unit) = onFailure(GoogleSignInError.CANCELLED)
    }

    private object UnusedSessionStore : SessionStore {
        override val sessions = flowOf(Sessions())
        override suspend fun save(sessions: Sessions) = error("not used")
    }

    private object UnusedFileStore : ReceiptFileStore {
        override suspend fun save(fileName: String, bytes: ByteArray) = error("not used")
        override suspend fun read(relativePath: String) = error("not used")
    }

    private object UnusedExtractor : PdfTextExtractor {
        override suspend fun extractText(pdf: ByteArray) = error("not used")
    }

    private object UnusedKeyStore : GeminiKeyStore {
        override val key = flowOf<String?>(null)
        override suspend fun save(key: String) = error("not used")
        override suspend fun clear() = error("not used")
    }

    /** Only what needs a real device is replaced (Android Context, files on disk); the rest is the real graph. */
    private val platformReplacements = module {
        single<SqlDriver> { createTestDriver() }
        single<ReceiptFileStore> { UnusedFileStore }
        single<PdfTextExtractor> { UnusedExtractor }
        single<GeminiKeyStore> { UnusedKeyStore }
        single<SessionStore> { UnusedSessionStore }
    }

    // Local KoinApplication: tests never touch the global Koin instance. Later modules override earlier ones.
    private val app = koinApplication {
        modules(
            listOf(platformServicesModule(PlatformServices(FakeGmailAuthorizer, FakeGoogleIdTokenProvider, "AIza-test"))) + appModules +
                platformReplacements
        )
    }

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
    fun accountRepository_resolvesWithTheApiKey() {
        app.koin.get<AccountRepository>()
    }

    @Test
    fun onboardingViewModel_resolves() {
        app.koin.get<OnboardingViewModel>()
    }

    @Test
    fun gmailApi_resolvesToKtorImplementation() {
        assertIs<KtorGmailApi>(app.koin.get<GmailApi>())
    }

    @Test
    fun settingsViewModel_resolvesWithTheRealGeminiApi() {
        app.koin.get<SettingsViewModel>()
    }

    @Test
    fun receiptsViewModel_resolvesWithTheRealRepositoryAndDatabase() {
        app.koin.get<ReceiptsViewModel>()
    }

    @Test
    fun chatViewModel_resolvesWithGeminiAndTheSqlTools() {
        app.koin.get<ChatViewModel>()
    }
}
