package com.cylonid.nativealpha.model

import com.google.gson.InstanceCreator
import java.lang.reflect.Type

class WebAppInstanceCreator : InstanceCreator<WebApp> {
    override fun createInstance(type: Type): WebApp {
        return WebApp("", Int.MAX_VALUE)
    }
}