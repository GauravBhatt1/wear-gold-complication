package com.goldrate.wear

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GoldRateService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val sample = "22K : ₹14,185 | 24K : ₹15,475"
        return when (type) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(sample).build(),
                PlainComplicationText.Builder("Gold Rate").build()
            ).build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(sample).build(),
                PlainComplicationText.Builder("Gold").build()
            ).build()
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val rateText = withContext(Dispatchers.IO) {
            try {
                val conn = URL("https://rate.freemiumstreem.workers.dev").openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                // Agar JSON me separate keys hain:
                val r22 = json.optString("rate22k", "").ifEmpty { json.optString("k22", "") }
                val r24 = json.optString("rate24k", "").ifEmpty { json.optString("k24", "") }

                if (r22.isNotEmpty() && r24.isNotEmpty()) {
                    "22K : ₹$r22 | 24K : ₹$r24"
                } else {
                    // Agar 'both' key me pehle se text hai, formatting normalize karein
                    val rawBoth = json.optString("both", "")
                    if (rawBoth.isNotEmpty()) {
                        rawBoth.replace("22K:", "22K : ").replace("24K:", "24K : ")
                    } else {
                        "Rates loading..."
                    }
                }
            } catch (e: Exception) {
                "Rate update error"
            }
        }

        return when (request.complicationType) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(rateText).build(),
                PlainComplicationText.Builder("Gold Rate").build()
            ).build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(rateText).build(),
                PlainComplicationText.Builder("Gold").build()
            ).build()
            else -> null
        }
    }
}
