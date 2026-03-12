package com.example.trailynapp.driver.complication

import android.content.Context
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.trailynapp.driver.services.HealthDataSyncService

/** Complicación que muestra la frecuencia cardíaca actual del conductor. */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return buildComplicationData("72♥", "72 BPM - Normal")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = getSharedPreferences(HealthDataSyncService.PREFS_NAME, Context.MODE_PRIVATE)
        val heartRate = prefs.getInt(HealthDataSyncService.KEY_HEART_RATE, 0)
        val status = prefs.getString(HealthDataSyncService.KEY_STATUS, "Sin señal") ?: "Sin señal"

        val shortText = if (heartRate > 0) "${heartRate}♥" else "--♥"
        val longText = if (heartRate > 0) "$heartRate BPM - $status" else "Sin señal"

        return buildComplicationData(shortText, longText)
    }

    private fun buildComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
}
