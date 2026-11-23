#include <cstdint>
#include <cstring>
#include <jni.h>
#include <new>
#include "third-party/ad-block/ad_block_client.h"

namespace {

inline AdBlockClient *toClient(jlong clientPointer) {
    return clientPointer == 0
           ? nullptr
           : reinterpret_cast<AdBlockClient *>(static_cast<intptr_t>(clientPointer));
}

inline jlong toHandle(const void *ptr) {
    return static_cast<jlong>(reinterpret_cast<intptr_t>(ptr));
}

// Replacement for NewStringUTF() that handles arbitrary UTF-8 bytes without Modified UTF-8 crashes.
jstring bytesToStringUTF(JNIEnv *env, const char *src) {
    if (!src) {
        return nullptr;
    }
    const size_t rawLen = strlen(src);
    const jsize len = static_cast<jsize>(rawLen);
    jstring encoding = env->NewStringUTF("UTF-8");
    jclass stringCls = env->FindClass("java/lang/String");
    if (!encoding || !stringCls) {
        if (encoding) env->DeleteLocalRef(encoding);
        if (stringCls) env->DeleteLocalRef(stringCls);
        return nullptr;
    }
    jmethodID methodId = env->GetMethodID(stringCls, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(len);
    jstring result = nullptr;
    if (methodId && bytes) {
        env->SetByteArrayRegion(bytes, 0, len, reinterpret_cast<const jbyte *>(src));
        if (!env->ExceptionCheck()) {
            result = static_cast<jstring>(env->NewObject(stringCls, methodId, bytes, encoding));
        }
    }
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        if (result) {
            env->DeleteLocalRef(result);
            result = nullptr;
        }
    }
    if (bytes) env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(encoding);
    env->DeleteLocalRef(stringCls);
    return result;
}

jobjectArray toStringArray(JNIEnv *env, const LinkedList<std::string> *rules) {
    if (!rules) {
        return nullptr;
    }
    jclass stringCls = env->FindClass("java/lang/String");
    if (!stringCls) {
        return nullptr;
    }
    jobjectArray array = env->NewObjectArray(rules->length(), stringCls, nullptr);
    env->DeleteLocalRef(stringCls);
    if (!array) {
        return nullptr;
    }
    int i = 0;
    for (const auto &r : *rules) {
        jstring elem = bytesToStringUTF(env, r.c_str());
        if (elem) {
            env->SetObjectArrayElement(array, i, elem);
            env->DeleteLocalRef(elem);
        }
        i++;
    }
    return array;
}

jobject createMatchResult(JNIEnv *env, bool shouldBlock, const char *matchedRule,
                          const char *matchedExceptionRule) {
    jclass matchResultClass = env->FindClass("io/github/edsuns/adblockclient/MatchResult");
    if (!matchResultClass) {
        return nullptr;
    }
    jmethodID initId = env->GetMethodID(matchResultClass, "<init>",
                                        "(ZLjava/lang/String;Ljava/lang/String;)V");
    if (!initId) {
        env->DeleteLocalRef(matchResultClass);
        return nullptr;
    }
    jstring matchedStr = bytesToStringUTF(env, matchedRule);
    jstring exceptionStr = bytesToStringUTF(env, matchedExceptionRule);
    jobject matchResult = env->NewObject(matchResultClass, initId,
                                         static_cast<jboolean>(shouldBlock),
                                         matchedStr,
                                         exceptionStr);
    if (matchedStr) env->DeleteLocalRef(matchedStr);
    if (exceptionStr) env->DeleteLocalRef(exceptionStr);
    env->DeleteLocalRef(matchResultClass);
    return matchResult;
}

} // namespace

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_createClient(JNIEnv *, jobject) {
    auto *client = new (std::nothrow) AdBlockClient();
    return toHandle(client);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_releaseBuffer(JNIEnv *, jobject,
                                                                jlong bufferPointer) {
    char *buffer = reinterpret_cast<char *>(static_cast<intptr_t>(bufferPointer));
    delete[] buffer;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_releaseClient(JNIEnv *, jobject,
                                                                jlong clientPointer,
                                                                jlong rawDataPointer,
                                                                jlong processedDataPointer) {
    delete toClient(clientPointer);
    delete[] reinterpret_cast<char *>(static_cast<intptr_t>(rawDataPointer));
    delete[] reinterpret_cast<char *>(static_cast<intptr_t>(processedDataPointer));
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_isGenericElementHidingEnabled(JNIEnv *,
                                                                                jobject,
                                                                                jlong clientPointer) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client) {
        return JNI_FALSE;
    }
    return static_cast<jboolean>(client->isGenericElementHidingEnabled);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_setGenericElementHidingEnabled(JNIEnv *,
                                                                                 jobject,
                                                                                 jlong clientPointer,
                                                                                 jboolean enabled) {
    AdBlockClient *client = toClient(clientPointer);
    if (client) {
        client->isGenericElementHidingEnabled = enabled;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_loadBasicData(JNIEnv *env,
                                                                jobject,
                                                                jlong clientPointer,
                                                                jbyteArray data,
                                                                jboolean preserveRules) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client || !data) {
        return 0;
    }
    const jsize dataLength = env->GetArrayLength(data);
    if (dataLength <= 0) {
        return 0;
    }
    char *dataChars = new (std::nothrow) char[static_cast<size_t>(dataLength) + 1];
    if (!dataChars) {
        return 0;
    }
    env->GetByteArrayRegion(data, 0, dataLength, reinterpret_cast<jbyte *>(dataChars));
    if (env->ExceptionCheck()) {
        delete[] dataChars;
        return 0;
    }
    dataChars[dataLength] = '\0';

    client->parse(dataChars, preserveRules);
    return toHandle(dataChars);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_loadProcessedData(JNIEnv *env,
                                                                    jobject,
                                                                    jlong clientPointer,
                                                                    jbyteArray data) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client || !data) {
        return 0;
    }
    const jsize dataLength = env->GetArrayLength(data);
    if (dataLength <= 0) {
        return 0;
    }
    char *dataChars = new (std::nothrow) char[static_cast<size_t>(dataLength) + 1];
    if (!dataChars) {
        return 0;
    }
    env->GetByteArrayRegion(data, 0, dataLength, reinterpret_cast<jbyte *>(dataChars));
    if (env->ExceptionCheck()) {
        delete[] dataChars;
        return 0;
    }
    dataChars[dataLength] = '\0';

    client->deserialize(dataChars);
    // We cannot delete dataChars here as adblock keeps a ptr to it.
    // Instead we send back a ptr ref so we can delete it later in releaseClient/releaseBuffer.
    return toHandle(dataChars);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getProcessedData(JNIEnv *env,
                                                                   jobject,
                                                                   jlong clientPointer) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client) {
        return env->NewByteArray(0);
    }

    int size = 0;
    char *data = client->serialize(&size, false);
    if (!data || size < 0) {
        delete[] data;
        return env->NewByteArray(0);
    }

    jbyteArray dataBytes = env->NewByteArray(size);
    if (dataBytes && size > 0) {
        env->SetByteArrayRegion(dataBytes, 0, size, reinterpret_cast<jbyte *>(data));
    }
    delete[] data;
    return dataBytes;
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getFiltersCount(JNIEnv *, jobject,
                                                                  jlong clientPointer) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client) {
        return 0;
    }
    return client->numFilters
           + client->numCosmeticFilters
           + client->numHtmlFilters
           + client->numScriptletFilters
           + client->numExceptionFilters
           + client->numNoFingerprintFilters
           + client->numNoFingerprintExceptionFilters
           + client->numNoFingerprintDomainOnlyFilters
           + client->numNoFingerprintAntiDomainOnlyFilters
           + client->numNoFingerprintDomainOnlyExceptionFilters
           + client->numNoFingerprintAntiDomainOnlyExceptionFilters
           + client->numHostAnchoredFilters
           + client->numHostAnchoredExceptionFilters;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_matches(JNIEnv *env, jobject,
                                                          jlong clientPointer, jstring url,
                                                          jstring firstPartyDomain,
                                                          jint filterOption) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client || !url || !firstPartyDomain) {
        return createMatchResult(env, false, nullptr, nullptr);
    }

    const char *urlChars = env->GetStringUTFChars(url, nullptr);
    const char *firstPartyDomainChars = env->GetStringUTFChars(firstPartyDomain, nullptr);

    bool shouldBlock = false;
    Filter *matchedFilter = nullptr;
    Filter *matchedExceptionFilter = nullptr;
    if (urlChars && firstPartyDomainChars) {
        shouldBlock = client->matches(urlChars, static_cast<FilterOption>(filterOption),
                                      firstPartyDomainChars, &matchedFilter,
                                      &matchedExceptionFilter);
    }

    if (urlChars) env->ReleaseStringUTFChars(url, urlChars);
    if (firstPartyDomainChars) env->ReleaseStringUTFChars(firstPartyDomain, firstPartyDomainChars);

    const char *matchedRule = matchedFilter ? matchedFilter->ruleDefinition : nullptr;
    const char *matchedExceptionRule = matchedExceptionFilter
                                       ? matchedExceptionFilter->ruleDefinition
                                       : nullptr;
    return createMatchResult(env, shouldBlock, matchedRule, matchedExceptionRule);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getElementHidingSelectors(JNIEnv *env,
                                                                            jobject,
                                                                            jlong clientPointer,
                                                                            jstring url) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client || !url) {
        return nullptr;
    }
    const char *urlChars = env->GetStringUTFChars(url, nullptr);
    if (!urlChars) {
        return nullptr;
    }
    const char *selectors = client->getElementHidingSelectors(urlChars);
    env->ReleaseStringUTFChars(url, urlChars);
    return bytesToStringUTF(env, selectors);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getExtendedCssSelectors(JNIEnv *env,
                                                                          jobject,
                                                                          jlong clientPointer,
                                                                          jstring url) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client || !url) {
        return nullptr;
    }
    const char *urlChars = env->GetStringUTFChars(url, nullptr);
    if (!urlChars) {
        return nullptr;
    }
    const LinkedList<std::string> *rules = client->getExtendedCssSelectors(urlChars);
    env->ReleaseStringUTFChars(url, urlChars);
    return toStringArray(env, rules);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getCssRules(JNIEnv *env,
                                                              jobject,
                                                              jlong clientPointer,
                                                              jstring url) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client || !url) {
        return nullptr;
    }
    const char *urlChars = env->GetStringUTFChars(url, nullptr);
    if (!urlChars) {
        return nullptr;
    }
    const LinkedList<std::string> *rules = client->getCssRules(urlChars);
    env->ReleaseStringUTFChars(url, urlChars);
    return toStringArray(env, rules);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getScriptlets(JNIEnv *env,
                                                                jobject,
                                                                jlong clientPointer,
                                                                jstring url) {
    AdBlockClient *client = toClient(clientPointer);
    if (!client || !url) {
        return nullptr;
    }
    const char *urlChars = env->GetStringUTFChars(url, nullptr);
    if (!urlChars) {
        return nullptr;
    }
    const LinkedList<std::string> *rules = client->getScriptlets(urlChars);
    env->ReleaseStringUTFChars(url, urlChars);
    return toStringArray(env, rules);
}
