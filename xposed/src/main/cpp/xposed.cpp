#include <jni.h>
#include <dobby.h>
#include "Utils/elf_util.h"
#include "Utils/log.h"
#include <string>
#include <vector>

typedef void (*hook0_t)(JNIEnv*, jclass, jobject, jclass, jint, jobject);
hook0_t orig_hook0 = nullptr;

void replaced_hook0(JNIEnv* env, jclass clazz, jobject reflectedMethodHandle, 
                    jclass declaredClass, jint slot, jobject additionalInfo) {
    
    LOGD("XposedBridge_hook0 called!");
    if (orig_hook0) {
        orig_hook0(env, clazz, reflectedMethodHandle, declaredClass, slot, additionalInfo);
    }
}
std::string getArtPath() {
    const char* libDir = (sizeof(void*) == 8) ? "lib64" : "lib";
    std::vector<std::string> searchPaths = {
        "/apex/com.android.art/" + std::string(libDir) + "/libart.so",
        "/apex/com.android.runtime/" + std::string(libDir) + "/libart.so",
        "/system/" + std::string(libDir) + "/libart.so"
    };
    for (const auto& path : searchPaths) {
        if (access(path.c_str(), R_OK) == 0) return path;
    }
    return "";
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;

    jclass bridgeClass = env->FindClass("de/robv/android/xposed/XposedBridge");
    if (!bridgeClass) {
        LOGE("XposedBridge class not found");
        return JNI_VERSION_1_6;
    }
    
    std::string artPath = getArtPath();
    LSPosed::ElfImg art_img(artPath.c_str());
    void* target_func = (void*)DobbySymbolResolver(nullptr, "Java_de_robv_android_xposed_XposedBridge_hook0");

    if (!target_func) {
        LOGE("Could not find Java_de_robv_android_xposed_XposedBridge_hook0 symbol");
        return JNI_VERSION_1_6;
    }
    int ret = DobbyHook(target_func, (dobby_dummy_func_t)replaced_hook0, (dobby_dummy_func_t *)&orig_hook0);

    if (ret == 0) {
        LOGD("Successfully hooked XposedBridge_hook0 at %p", target_func);
    } else {
        LOGE("Failed to hook XposedBridge_hook0, error code: %d", ret);
    }

    return JNI_VERSION_1_6;
}
