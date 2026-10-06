package com.majidbahmani.cesto.feature.chat.data.tools

import com.majidbahmani.cesto.feature.chat.domain.model.ReceiptToolNames
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** The tools as Gemini function declarations: the only way the model can look at the receipts. */
val receiptToolDeclarations: JsonArray = buildJsonArray {
    addJsonObject {
        put("name", ReceiptToolNames.FIND_PRODUCTS)
        put(
            "description",
            "Finds products bought at Continente whose name or category contains any keyword. " +
                "Names are Portuguese (e.g. iogurte, leite, ovos, banana): use Portuguese stems without accents, " +
                "e.g. \"iogurt\" for yogurt. Returns at most 50 products with their ids.",
        )
        putJsonObject("parameters") {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("keywords") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                    put("description", "1 to 5 keywords.")
                }
            }
            putJsonArray("required") { add("keywords") }
        }
    }
    addJsonObject {
        put("name", ReceiptToolNames.SUM_QUANTITY)
        put(
            "description",
            "How much of some products was bought in a period: per product and in total, " +
                "in units (packs and single units, e.g. a 4x125 g pack is 4 single units) and in kg for weighed products.",
        )
        putJsonObject("parameters") {
            put("type", "OBJECT")
            putJsonObject("properties") {
                productIds("Ids from findProducts.")
                dateRange()
            }
            putJsonArray("required") { add("productIds"); add("from"); add("to") }
        }
    }
    addJsonObject {
        put("name", ReceiptToolNames.SUM_SPENDING)
        put(
            "description",
            "Euros spent in a period. Without productIds: everything paid (receipt totals). " +
                "With productIds: only those products, per product and in total.",
        )
        putJsonObject("parameters") {
            put("type", "OBJECT")
            putJsonObject("properties") {
                productIds("Optional ids from findProducts; leave out for all spending.")
                dateRange()
            }
            putJsonArray("required") { add("from"); add("to") }
        }
    }
    addJsonObject {
        put("name", ReceiptToolNames.TOP_PRODUCTS)
        put("description", "The products bought most in a period, by quantity or by euros spent.")
        putJsonObject("parameters") {
            put("type", "OBJECT")
            putJsonObject("properties") {
                dateRange()
                putJsonObject("by") {
                    put("type", "STRING")
                    putJsonArray("enum") { add("quantity"); add("spending") }
                }
                putJsonObject("limit") {
                    put("type", "INTEGER")
                    put("description", "1 to 20, default 10.")
                }
            }
            putJsonArray("required") { add("from"); add("to"); add("by") }
        }
    }
    addJsonObject {
        put("name", ReceiptToolNames.LIST_RECEIPTS)
        put("description", "The receipts of a period (newest first, at most 50): date, total paid and number of items.")
        putJsonObject("parameters") {
            put("type", "OBJECT")
            putJsonObject("properties") { dateRange() }
            putJsonArray("required") { add("from"); add("to") }
        }
    }
}

private fun JsonObjectBuilder.productIds(description: String) {
    putJsonObject("productIds") {
        put("type", "ARRAY")
        putJsonObject("items") { put("type", "INTEGER") }
        put("description", "$description At most 50.")
    }
}

private fun JsonObjectBuilder.dateRange() {
    putJsonObject("from") {
        put("type", "STRING")
        put("description", "First day, YYYY-MM-DD.")
    }
    putJsonObject("to") {
        put("type", "STRING")
        put("description", "Last day (included), YYYY-MM-DD.")
    }
}
