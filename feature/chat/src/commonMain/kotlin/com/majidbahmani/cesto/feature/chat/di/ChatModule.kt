package com.majidbahmani.cesto.feature.chat.di

import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.feature.chat.data.agent.GeminiAgentModel
import com.majidbahmani.cesto.feature.chat.data.agent.ReceiptAgentPrompt
import com.majidbahmani.cesto.feature.chat.data.tools.SqlReceiptTools
import com.majidbahmani.cesto.feature.chat.domain.model.AgentModel
import com.majidbahmani.cesto.feature.chat.domain.model.ReceiptTools
import com.majidbahmani.cesto.feature.chat.domain.usecase.AskQuestionUseCase
import com.majidbahmani.cesto.feature.chat.domain.usecase.ObserveAssistantReadyUseCase
import com.majidbahmani.cesto.feature.chat.presentation.viewmodel.ChatViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Clock

/** Needs the IO dispatcher (:core), CestoDatabase (:database), GeminiApi + key store (:llm). */
val chatModule = module {
    single<ReceiptTools> { SqlReceiptTools(database = get(), ioDispatcher = get(IoDispatcher)) }
    single {
        ReceiptAgentPrompt(
            database = get(),
            ioDispatcher = get(IoDispatcher),
            currentTimeMillis = { Clock.System.now().toEpochMilliseconds() },
        )
    }
    single<AgentModel> { GeminiAgentModel(gemini = get(), keys = get(), prompt = get()) }
    factory { AskQuestionUseCase(model = get(), tools = get()) }
    factory { ObserveAssistantReadyUseCase(model = get()) }
    viewModel { ChatViewModel(observeAssistantReady = get(), askQuestion = get()) }
}
