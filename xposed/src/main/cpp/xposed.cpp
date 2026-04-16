#include <jni.h>
#include <dobby.h>
#include "Utils/elf_util.h"
#include "Utils/log.h"
#include <string>
#include <vector>
#include <unistd.h>

typedef jobject (*hook0_t)(JNIEnv*, jclass, jobject, jobject, jobject);
hook0_t orig_hook0 = nullptr;

jobject replaced_hook0(JNIEnv* env, jclass clazz, jobject context, jobject member, jobject callback) {
    LOGD("XposedBridge_hook0 called for member!");
    if (orig_hook0) {
        return orig_hook0(env, clazz, context, member, callback);
    }
    return nullptr; 
}
static JNINativeMethod gMethods[] = {
    {
        "hook0", 
        "(Ljava/lang/Object;Ljava/lang/reflect/Member;Ljava/lang/reflect/Method;)Ljava/lang/reflect/Method;", 
        (void*)replaced_hook0
    }
};
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
    if (env->RegisterNatives(bridgeClass, gMethods, 1) < 0) {
        LOGE("RegisterNatives failed for hook0");
        return JNI_ERR;
    }
    LOGD("Successfully registered hook0");
    return JNI_VERSION_1_6;
}
