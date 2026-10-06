package com.majidbahmani.cesto.feature.receipts.data.remote

/**
 * Where Continente receipts come from. Only the Cartão Continente e-mail for now: a PDF
 * "Fatura Simplificada" named like Fatura_Cartao_Continente_20261005_2122.pdf.
 * Other stores (or online orders) get their own config later.
 */
object ContinenteReceipts {
    const val SENDER = "noreply@cartaocontinente.pt"

    /** Gmail search syntax; `newer_than:3m` = the last 3 months. */
    fun gmailQuery(lookBackMonths: Int): String {
        require(lookBackMonths > 0) { "lookBackMonths must be positive" }
        return "from:$SENDER has:attachment filename:pdf newer_than:${lookBackMonths}m"
    }
}
