package com.example.hourlychime

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

class PomodoroTileService : TileService() {

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("PomodoroTileService", "onCreate")
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val future = SettableFuture.create<TileBuilders.Tile>()
        val state = PomodoroManager.getState(applicationContext)
        val endTime = PomodoroManager.getEndTime(applicationContext)
        
        // Convert tiles device config to protolayout device parameters
        val tilesConfig = requestParams.deviceConfiguration
        val deviceParams = DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(tilesConfig.screenWidthDp)
            .setScreenHeightDp(tilesConfig.screenHeightDp)
            .setScreenDensity(tilesConfig.screenDensity)
            .setScreenShape(tilesConfig.screenShape)
            .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(renderTile(state, endTime, deviceParams))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
        
        future.set(tile)
        return future
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val future = SettableFuture.create<ResourceBuilders.Resources>()
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
        future.set(resources)
        return future
    }

    private fun renderTile(
        state: PomodoroManager.State,
        endTime: Long,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        val rootBuilder = PrimaryLayout.Builder(deviceParameters)
            .setResponsiveContentInsetEnabled(true)

        val title = if (state == PomodoroManager.State.IDLE) "Tempo Focus" else if (state == PomodoroManager.State.WORK) "Focusing" else "On Break"
        
        val contentBuilder = LayoutElementBuilders.Column.Builder()
            .addContent(
                Text.Builder(this, title)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f)).build())

        if (state == PomodoroManager.State.IDLE) {
            contentBuilder.addContent(
                Button.Builder(this, clickAction())
                    .setTextContent("Start")
                    .build()
            )
        } else {
            val remaining = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
            val minutes = (remaining / 1000) / 60
            contentBuilder.addContent(
                Text.Builder(this, "${minutes}m left")
                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                    .build()
            )
            contentBuilder.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8f)).build())
            contentBuilder.addContent(
                Button.Builder(this, clickAction())
                    .setTextContent("Open")
                    .build()
            )
        }

        return rootBuilder.setContent(contentBuilder.build()).build()
    }

    private fun clickAction(): ModifiersBuilders.Clickable {
        return ModifiersBuilders.Clickable.Builder()
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName("com.example.hourlychime")
                            .setClassName("com.example.hourlychime.presentation.MainActivity")
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
