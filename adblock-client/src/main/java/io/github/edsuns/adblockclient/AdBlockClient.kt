/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.edsuns.adblockclient

import android.net.Uri
import timber.log.Timber

/**
 * Modified by Edsuns@qq.com.
 *
 * Description: In `duckduckgo/Android`, `tag/5.38.1` is the last version that has the implement of AdBlockClient.
 *
 * Reference: [github.com/duckduckgo/Android/releases/tag/5.38.1](https://github.com/duckduckgo/Android/releases/tag/5.38.1)
 */
open class AdBlockClient(
    override val id: String,
) : Client,
    AutoCloseable {
    private var nativeClientPointer: Long
    private var rawDataPointer: Long
    private var processedDataPointer: Long

    init {
        nativeClientPointer = createClient()
        rawDataPointer = 0
        processedDataPointer = 0
    }

    private external fun createClient(): Long

    private external fun releaseBuffer(bufferPointer: Long)

    /**
     * @param data requires UTF-8 bytes
     */
    @Synchronized
    fun loadBasicData(
        data: ByteArray,
        preserveRules: Boolean = false,
    ) {
        check(nativeClientPointer != 0L) { "AdBlockClient is closed" }
        val timestamp = System.currentTimeMillis()
        Timber.d("Loading basic data for $id")
        if (rawDataPointer != 0L) {
            releaseBuffer(rawDataPointer)
            rawDataPointer = 0L
        }
        rawDataPointer = loadBasicData(nativeClientPointer, data, preserveRules)
        Timber.d("Loading basic data for $id completed in ${System.currentTimeMillis() - timestamp}ms")
    }

    override var isGenericElementHidingEnabled: Boolean
        get() = if (nativeClientPointer == 0L) false else isGenericElementHidingEnabled(nativeClientPointer)
        set(value) {
            if (nativeClientPointer != 0L) {
                setGenericElementHidingEnabled(nativeClientPointer, value)
            }
        }

    private external fun isGenericElementHidingEnabled(clientPointer: Long): Boolean

    private external fun setGenericElementHidingEnabled(
        clientPointer: Long,
        enabled: Boolean,
    )

    private external fun loadBasicData(
        clientPointer: Long,
        data: ByteArray,
        preserveRules: Boolean,
    ): Long

    @Synchronized
    fun loadProcessedData(data: ByteArray) {
        check(nativeClientPointer != 0L) { "AdBlockClient is closed" }
        val timestamp = System.currentTimeMillis()
        Timber.d("Loading preprocessed data for $id")
        if (processedDataPointer != 0L) {
            releaseBuffer(processedDataPointer)
            processedDataPointer = 0L
        }
        processedDataPointer = loadProcessedData(nativeClientPointer, data)
        Timber.d("Loading preprocessed data for $id completed in ${System.currentTimeMillis() - timestamp}ms")
    }

    private external fun loadProcessedData(
        clientPointer: Long,
        data: ByteArray,
    ): Long

    fun getProcessedData(): ByteArray = if (nativeClientPointer == 0L) ByteArray(0) else getProcessedData(nativeClientPointer)

    private external fun getProcessedData(clientPointer: Long): ByteArray

    fun getFiltersCount(): Int = if (nativeClientPointer == 0L) 0 else getFiltersCount(nativeClientPointer)

    private external fun getFiltersCount(clientPointer: Long): Int

    override fun matches(
        url: String,
        documentUrl: String,
        resourceType: ResourceType,
    ): MatchResult {
        val ptr = nativeClientPointer
        if (ptr == 0L) return MatchResult(false, null, null)
        val firstPartyDomain = documentUrl.baseHost() ?: return MatchResult(false, null, null)
        return matches(ptr, url, firstPartyDomain, resourceType.filterOption)
    }

    private external fun matches(
        clientPointer: Long,
        url: String,
        firstPartyDomain: String,
        filterOption: Int,
    ): MatchResult

    override fun getElementHidingSelectors(url: String): String? =
        nativeClientPointer.takeIf { it != 0L }?.let { getElementHidingSelectors(it, url) }

    override fun getExtendedCssSelectors(url: String): Array<String>? =
        nativeClientPointer.takeIf { it != 0L }?.let { getExtendedCssSelectors(it, url) }

    override fun getCssRules(url: String): Array<String>? = nativeClientPointer.takeIf { it != 0L }?.let { getCssRules(it, url) }

    override fun getScriptlets(url: String): Array<String>? = nativeClientPointer.takeIf { it != 0L }?.let { getScriptlets(it, url) }

    private external fun getElementHidingSelectors(
        clientPointer: Long,
        url: String,
    ): String?

    private external fun getExtendedCssSelectors(
        clientPointer: Long,
        url: String,
    ): Array<String>?

    private external fun getCssRules(
        clientPointer: Long,
        url: String,
    ): Array<String>?

    private external fun getScriptlets(
        clientPointer: Long,
        url: String,
    ): Array<String>?

    @Synchronized
    override fun close() {
        val clientPtr = nativeClientPointer
        val rawPtr = rawDataPointer
        val processedPtr = processedDataPointer
        if (clientPtr == 0L && rawPtr == 0L && processedPtr == 0L) {
            return
        }
        nativeClientPointer = 0L
        rawDataPointer = 0L
        processedDataPointer = 0L
        releaseClient(clientPtr, rawPtr, processedPtr)
    }

    protected fun finalize() {
        close()
    }

    private external fun releaseClient(
        clientPointer: Long,
        rawDataPointer: Long,
        processedDataPointer: Long,
    )

    private fun String.baseHost(): String? = Uri.parse(this).host?.removePrefix("www.")

    companion object {
        init {
            System.loadLibrary("adblock-client")
        }
    }
}
