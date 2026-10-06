package com.majidbahmani.cesto.feature.receipts.data.remote

/**
 * Hand-written in the documented shape of the Gmail API (developers.google.com/gmail/api/reference/rest),
 * not a real capture: replace with a captured, anonymised Continente email once the format is confirmed.
 * Multipart message: HTML body + a PDF + a PDF sent as octet-stream (nested) + a PNG logo.
 */
internal const val MESSAGE_WITH_PDFS_DOCUMENTED_SHAPE = """
{
  "id": "18c2a0f1",
  "threadId": "18c2a0f1",
  "internalDate": "1788432000000",
  "snippet": "Obrigado pela sua compra",
  "payload": {
    "partId": "",
    "mimeType": "multipart/mixed",
    "filename": "",
    "headers": [
      { "name": "From", "value": "Continente <noreply@example.pt>" },
      { "name": "Subject", "value": "A sua fatura" }
    ],
    "body": { "size": 0 },
    "parts": [
      {
        "partId": "0",
        "mimeType": "multipart/alternative",
        "filename": "",
        "body": { "size": 0 },
        "parts": [
          { "partId": "0.0", "mimeType": "text/html", "filename": "", "body": { "size": 12, "data": "PHA-T2zDoTwvcD4" } },
          { "partId": "0.1", "mimeType": "application/octet-stream", "filename": "talao-2.PDF", "body": { "attachmentId": "ANGjdJ_b", "size": 2048 } }
        ]
      },
      { "partId": "1", "mimeType": "application/pdf", "filename": "fatura.pdf", "body": { "attachmentId": "ANGjdJ_a", "size": 40123 } },
      { "partId": "2", "mimeType": "image/png", "filename": "logo.png", "body": { "attachmentId": "ANGjdJ_c", "size": 900 } }
    ]
  }
}
"""

/** `messages.list` with a next page. */
internal const val MESSAGE_LIST_DOCUMENTED_SHAPE = """
{
  "messages": [ { "id": "m2", "threadId": "t2" }, { "id": "m1", "threadId": "t1" } ],
  "nextPageToken": "page-2",
  "resultSizeEstimate": 2
}
"""

/** `messages.list` when nothing matches: Gmail leaves `messages` out entirely. */
internal const val MESSAGE_LIST_EMPTY = """{ "resultSizeEstimate": 0 }"""

/** `attachments.get`: base64url without padding ("%PDF-1.7" → "JVBERi0xLjc"). */
internal const val ATTACHMENT_DOCUMENTED_SHAPE = """{ "size": 8, "data": "JVBERi0xLjc" }"""
