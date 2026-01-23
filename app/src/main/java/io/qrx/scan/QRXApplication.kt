package io.qrx.scan

import android.app.Application
import io.qrx.scan.data.AppDatabase
import io.qrx.scan.util.CrashHandler

class QRXApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.instance.init(this)
    }
}
