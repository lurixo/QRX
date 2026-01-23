package io.qrx.scan.util

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.qrx.scan.R
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var context: Context? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    companion object {
        val instance: CrashHandler by lazy { CrashHandler() }
    }

    fun init(context: Context) {
        this.context = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        saveCrashLog(throwable)

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashLog(throwable: Throwable) {
        val ctx = context ?: return

        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "QRX_crash_$timestamp.txt"

            val logContent = buildString {
                appendLine(ctx.getString(R.string.crash_log_header))
                appendLine()
                appendLine(ctx.getString(R.string.crash_section_time))
                appendLine(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date()))
                appendLine()

                appendLine(ctx.getString(R.string.crash_section_device))
                appendLine(ctx.getString(R.string.crash_brand, Build.BRAND))
                appendLine(ctx.getString(R.string.crash_model, Build.MODEL))
                appendLine(ctx.getString(R.string.crash_device, Build.DEVICE))
                appendLine(ctx.getString(R.string.crash_android_version, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
                appendLine()

                appendLine(ctx.getString(R.string.crash_section_app))
                try {
                    val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                    appendLine(ctx.getString(R.string.crash_package, ctx.packageName))
                    appendLine(ctx.getString(R.string.crash_version_name, packageInfo.versionName))
                    appendLine(ctx.getString(R.string.crash_version_code, packageInfo.longVersionCode))
                } catch (e: PackageManager.NameNotFoundException) {
                    appendLine(ctx.getString(R.string.crash_cannot_get_app_info))
                }
                appendLine()

                appendLine(ctx.getString(R.string.crash_section_exception))
                appendLine(ctx.getString(R.string.crash_exception_type, throwable.javaClass.name))
                appendLine(ctx.getString(R.string.crash_exception_message, throwable.message ?: ""))
                appendLine()

                appendLine(ctx.getString(R.string.crash_section_stack))
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                appendLine(sw.toString())

                var cause = throwable.cause
                while (cause != null) {
                    appendLine()
                    appendLine(ctx.getString(R.string.crash_caused_by))
                    appendLine(ctx.getString(R.string.crash_exception_type, cause.javaClass.name))
                    appendLine(ctx.getString(R.string.crash_exception_message, cause.message ?: ""))
                    val csw = StringWriter()
                    val cpw = PrintWriter(csw)
                    cause.printStackTrace(cpw)
                    appendLine(csw.toString())
                    cause = cause.cause
                }

                appendLine()
                appendLine(ctx.getString(R.string.crash_log_footer))
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QRX")
            }

            val uri = ctx.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                ctx.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(logContent.toByteArray())
                }
            }

        } catch (e: Exception) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val logDir = File(ctx.filesDir, "crash_logs")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                val logFile = File(logDir, "crash_$timestamp.txt")

                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                logFile.writeText("${ctx.getString(R.string.crash_time_prefix)} $timestamp\n\n${sw}")
            } catch (e2: Exception) {
                android.util.Log.e("CrashHandler", "Failed to save crash log", e2)
            }
        }
    }
}
