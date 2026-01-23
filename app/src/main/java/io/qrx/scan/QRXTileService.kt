package io.qrx.scan

import android.content.Intent
import android.service.quicksettings.TileService

class QRXTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = android.service.quicksettings.Tile.STATE_INACTIVE
            label = getString(R.string.tile_scan_label)
            contentDescription = getString(R.string.tile_scan_description)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        startActivityAndCollapse(android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, ScanActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("from_tile", true)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        ))
    }
}
