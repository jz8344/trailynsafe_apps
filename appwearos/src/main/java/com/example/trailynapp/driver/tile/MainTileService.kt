package com.example.trailynapp.driver.tile

import android.content.Context
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.trailynapp.driver.services.HealthDataSyncService
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

private const val RESOURCES_VERSION = "1"

/** Tile que muestra la frecuencia cardíaca actual del conductor. */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ) = tile(requestParams, this)
}

private fun resources(
    requestParams: RequestBuilders.ResourcesRequest
): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): TileBuilders.Tile {
    val singleTileTimeline = TimelineBuilders.Timeline.Builder()
        .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(requestParams, context))
                        .build()
                )
                .build()
        )
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline)
        .setFreshnessIntervalMillis(10_000L) // Actualizar cada 10 segundos
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): LayoutElementBuilders.LayoutElement {
    val prefs = context.getSharedPreferences(
        HealthDataSyncService.PREFS_NAME, Context.MODE_PRIVATE
    )
    val heartRate = prefs.getInt(HealthDataSyncService.KEY_HEART_RATE, 0)
    val status = prefs.getString(HealthDataSyncService.KEY_STATUS, "Sin señal") ?: "Sin señal"

    val bpmText = if (heartRate > 0) "$heartRate BPM" else "-- BPM"

    // Color según estado de salud
    val textColor = when {
        heartRate <= 0 -> Colors.DEFAULT.onSurface
        heartRate < 60 -> android.graphics.Color.parseColor("#1565C0") // azul (bradicardia)
        heartRate in 60..100 -> android.graphics.Color.parseColor("#2E7D32") // verde (normal)
        heartRate in 101..120 -> android.graphics.Color.parseColor("#E65100") // naranja (elevado)
        else -> android.graphics.Color.parseColor("#B71C1C") // rojo (taquicardia)
    }

    return PrimaryLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setPrimaryLabelTextContent(
            Text.Builder(context, "❤️ Frecuencia")
                .setColor(argb(Colors.DEFAULT.onSurface))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
        .setContent(
            Text.Builder(context, bpmText)
                .setColor(argb(textColor))
                .setTypography(Typography.TYPOGRAPHY_TITLE2)
                .build()
        )
        .setSecondaryLabelTextContent(
            Text.Builder(context, status)
                .setColor(argb(Colors.DEFAULT.onSurface))
                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                .build()
        )
        .build()
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context)
}
