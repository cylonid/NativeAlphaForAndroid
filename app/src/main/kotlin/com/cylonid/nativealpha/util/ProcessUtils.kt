package com.cylonid.nativealpha.util

import android.app.ActivityManager
import android.os.Process

object ProcessUtils {
    @JvmStatic
    fun closeAllWebAppsAndProcesses(activityManager: ActivityManager) {
        for (task in activityManager.appTasks.orEmpty()) {
            val id = task.taskInfo?.baseIntent?.getIntExtra(Const.INTENT_WEBAPPID, -1) ?: -1
            if (id != -1) task.finishAndRemoveTask()
        }
        for (processInfo in activityManager.runningAppProcesses.orEmpty()) {
            val processName = processInfo.processName ?: continue
            if (processName.contains("web_sandbox")) {
                Process.killProcess(processInfo.pid)
            }
        }
    }

    fun killWebSandbox(
        id: Int,
        activityManager: ActivityManager,
    ) {
        for (processInfo in activityManager.runningAppProcesses.orEmpty()) {
            val processName = processInfo.processName ?: continue
            if (processName.endsWith(":web_sandbox_$id")) {
                Process.killProcess(processInfo.pid)
            }
        }
    }
}
