package com.cylonid.nativealpha.model.deserializer

import com.cylonid.nativealpha.model.AdblockConfig
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.Const
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import timber.log.Timber
import java.lang.reflect.Type

class WebAppDeserializer : JsonDeserializer<WebApp> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): WebApp {
        val obj = json.asJsonObject
        val webapp = plainGson.fromJson(obj, WebApp::class.java)
        if (!obj.has("isUseBuiltinPdfViewer") || obj.get("isUseBuiltinPdfViewer").isJsonNull) {
            webapp.isUseBuiltinPdfViewer = true
        }
        patchDataVersion1500(webapp, obj)
        return webapp
    }

    /**
     *  With release v1.5.0 (code 1500), we added the array "adBlockSettings" to WebApp.
     If the value in JSON string is null, we set an empty array for usual web apps and the default adblock provider for the global web app.
     */
    private fun patchDataVersion1500(
        webapp: WebApp,
        obj: JsonObject,
    ) {
        var adblockKeyPresent = false

        val parsedAdblockSettings =
            if (obj.has("adBlockSettings") && obj.get("adBlockSettings").isJsonArray) {
                val array = obj.getAsJsonArray("adBlockSettings")
                adblockKeyPresent = true
                array.mapNotNull { item ->
                    try {
                        val configObj = item.asJsonObject
                        val label = configObj.get("label")?.asString ?: return@mapNotNull null
                        val value = configObj.get("value")?.asString ?: return@mapNotNull null
                        AdblockConfig(label, value)
                    } catch (e: Exception) {
                        Timber.w(e, "Skipping malformed adblock config entry")
                        null
                    }
                }
            } else {
                emptyList()
            }

        webapp.adBlockSettings = parsedAdblockSettings.toMutableList()
        if (webapp.ID == Int.MAX_VALUE && !adblockKeyPresent) {
            webapp.adBlockSettings = Const.getDefaultAdBlockConfig()
        }
        webapp.adBlockSettings = webapp.adBlockSettings.take(8).toMutableList()
    }

    private companion object {
        val plainGson = Gson()
    }
}
