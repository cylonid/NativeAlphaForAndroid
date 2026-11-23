package com.cylonid.nativealpha.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow

@Implements(ActivityManager.AppTask::class)
class ShadowTestAppTask {
    var taskInfoValue: ActivityManager.RecentTaskInfo = ActivityManager.RecentTaskInfo()
    var finished: Boolean = false

    @Implementation
    fun getTaskInfo(): ActivityManager.RecentTaskInfo = taskInfoValue

    @Implementation
    fun finishAndRemoveTask() {
        finished = true
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowTestAppTask::class])
class ProcessUtilsTest {
    @Test
    fun testCloseAllWebAppsFinishesOnlyWebAppTasks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val am = context.getSystemService(ActivityManager::class.java)!!
        val shadowAm = Shadows.shadowOf(am)

        val webAppTask = Shadow.newInstanceOf(ActivityManager.AppTask::class.java)
        val webAppShadow = Shadow.extract<ShadowTestAppTask>(webAppTask)
        webAppShadow.taskInfoValue =
            ActivityManager.RecentTaskInfo().apply {
                baseIntent = Intent().putExtra(Const.INTENT_WEBAPPID, 3)
            }

        val mainTask = Shadow.newInstanceOf(ActivityManager.AppTask::class.java)
        val mainShadow = Shadow.extract<ShadowTestAppTask>(mainTask)
        mainShadow.taskInfoValue =
            ActivityManager.RecentTaskInfo().apply {
                baseIntent = Intent()
            }

        shadowAm.setAppTasks(listOf(webAppTask, mainTask))
        shadowAm.setProcesses(
            listOf(
                ActivityManager.RunningAppProcessInfo().apply {
                    processName = "com.cylonid.nativealpha"
                    pid = 99999
                },
            ),
        )

        ProcessUtils.closeAllWebAppsAndProcesses(am)

        assertTrue(webAppShadow.finished)
        assertFalse(mainShadow.finished)
    }
}
