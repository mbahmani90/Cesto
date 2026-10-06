package com.majidbahmani.cesto.feature.chat.presentation.mapper

import com.majidbahmani.cesto.feature.chat.domain.model.ReceiptToolNames
import com.majidbahmani.cesto.feature.chat.domain.model.ToolCall
import com.majidbahmani.cesto.feature.chat.presentation.model.ThinkingStep

internal fun ToolCall.toThinkingStep(): ThinkingStep = when (name) {
    ReceiptToolNames.FIND_PRODUCTS, ReceiptToolNames.SEMANTIC_SEARCH -> ThinkingStep.FINDING_PRODUCTS
    ReceiptToolNames.SUM_QUANTITY -> ThinkingStep.COUNTING
    ReceiptToolNames.SUM_SPENDING -> ThinkingStep.ADDING_SPENDING
    ReceiptToolNames.TOP_PRODUCTS -> ThinkingStep.RANKING
    ReceiptToolNames.LIST_RECEIPTS -> ThinkingStep.LISTING_RECEIPTS
    else -> ThinkingStep.THINKING
}
