package com.majidbahmani.cesto.feature.receipts.data.parser

// Invented receipts in the layout of a real Cartão Continente "Fatura Simplificada": no real NIF,
// card number, receipt number or ATCUD. Two orders of the same receipt, as the extractors produce them.

/** PdfBox-Android: lines in reading order, each price next to its item. */
internal const val PDFBOX_RECEIPT = """MDL Exemplo
MODELO CONT.HIPERM.,SA
NIF: PT500000000|C.S:403.827.000,00|EUR
Fatura Simplificada Original
Nro:FS ABC123/000001 05/10/2026 21:22 | NIF:PT123456789
IVA DESCRICAO VALOR
Laticinios/Beb. Veg.:
(A) LEITE PAST GORDO 1L 1,19
Frutas e Legumes:
(A) BANANA 
0,760 X 1,19 0,90
SUBTOTAL 2,09
Desconto Cartao Utilizado 0,21
TOTAL A PAGAR 1,88
Cartao Cliente 1,88
Cartao cliente nº XXXXXXXX0000X
ATCUD:ABCD1234-000001
IVA INCLUIDO"""

/** PDFKit (iOS): two-column layout, item names and prices land in different places. */
internal const val PDFKIT_RECEIPT = """MDL Exemplo
MODELO CONT.HIPERM.,SA
Fatura Simplificada Original
Nro:FS ABC123/000001 05/10/2026 21:22 | NIF:PT123456789
IVA DESCRICAO VALOR
Laticinios/Beb. Veg.:
(A) LEITE PAST GORDO 1L Frutas e Legumes:
(A) BANANA
1,19
0,760 X 1,19 0,90
SUBTOTAL 2,09
Desconto Cartao Utilizado 0,21
TOTAL A PAGAR 1,88
Cartao Cliente 1,88
ATCUD:ABCD1234-000001"""
