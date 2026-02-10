package io.qrx.scan.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "io.qrx.scan",
            maxIterations = 5,
            stableIterations = 3
        ) {
            pressHome()
            startActivityAndWait()

            device.wait(Until.hasObject(By.pkg("io.qrx.scan")), 5_000)

            device.findObject(By.desc("History"))?.click()
            device.waitForIdle()

            device.pressBack()
            device.waitForIdle()
        }
    }
}
