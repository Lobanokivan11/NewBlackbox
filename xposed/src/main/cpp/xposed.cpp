#include <jni.h>
#include <dobby.h>
#include <lsplant.hpp>
#include <sys/sysconf.h>
#include <sys/mman.h>
#include "Utils/elf_util.h"
#include "Utils/log.h"
#include <string>
#include <functional>
#include <string_view>
#include <unistd.h>

void *inlineHooker(void *targetFunc, void *replaceFunc) {
    auto pageSize = sysconf(_SC_PAGE_SIZE);
    auto funcAddress = ((uintptr_t) targetFunc) & (-pageSize);
    mprotect((void *) funcAddress, pageSize, PROT_READ | PROT_WRITE | PROT_EXEC);

    void *originalFunc;
    if (DobbyHook(targetFunc, (dobby_dummy_func_t) replaceFunc, (dobby_dummy_func_t *) &originalFunc) == 0) {
        return originalFunc;
    }
    return nullptr;
}

bool inlineUnHooker(void *originalFunc) {
    return DobbyDestroy(originalFunc) == 0;
}

std::string getArtPath() {
    std::string libDir = (sizeof(void*) == 8) ? "lib64" : "lib";
    std::vector<std::string> searchPaths = {
        "/apex/com.android.art/" + libDir + "/libart.so",
        "/apex/com.android.runtime/" + libDir + "/libart.so",
        "/system/" + libDir + "/libart.so",
        "/system/apex/com.android.art/" + libDir + "/libart.so"
    };
    for (const auto& path : searchPaths) {
        if (access(path.c_str(), R_OK) == 0) {
            return path;
        }
    }
    return "";
}

extern "C"
JNIEXPORT jobject JNICALL
Java_de_robv_android_xposed_XposedBridge_hook0(JNIEnv *env, jclass clazz, jobject context, jobject originalMethod, jobject callbackMethod) {
    return lsplant::Hook(env, originalMethod, context, callbackMethod);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_unhook0(JNIEnv *env, jclass clazz, jobject targetMember) {
    return lsplant::UnHook(env, targetMember);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    std::string artPath = getArtPath();
    if (artPath.empty()) {
        LOGE("libart.so path not found!");
        return JNI_ERR;
    }
    LOGD("Loading ART from: %s", artPath.c_str());
    static LSPosed::ElfImg art(artPath.c_str());
    if (!art.isValid()) {
        LOGE("Failed to load libart.so: %s", artPath.c_str());
        return JNI_ERR;
    }
    lsplant::InitInfo initInfo {
            .inline_hooker = inlineHooker,
            .inline_unhooker = inlineUnHooker,
            .art_symbol_resolver = [](std::string_view symbol) -> void * {
                void* addr = art.getSymbAddress(symbol.data());
                if (!addr) {
                    LOGE("Symbol NOT found: %s", symbol.data());
                } else {
                    LOGD("Symbol found: %s at %p", symbol.data(), addr);
                }
                return addr;
            },
            .art_symbol_prefix_resolver = [](auto symbol) -> void* {
                void* addr = art.getSymbPrefixFirstOffset(symbol);
                std::string sym_str(symbol);
                if (!addr) {
                    LOGE("Prefix symbol NOT found: %s", sym_str.c_str());
                } else {
                    LOGD("Prefix symbol found: %s at %p", sym_str.c_str(), addr);
                }
                return addr;
            },
            .generated_class_name = "org/lsposed/lsplant/GeneratedStub",
            .generated_source_name = "LSPlant",
            .generated_method_name = "hookStub"
    };
    if (!lsplant::Init(env, initInfo)) {
        LOGE("LSPlant core initialization failed!");
        return JNI_ERR;
    }
    LOGI("LSPlant initialized successfully on %s", artPath.c_str());
    return JNI_VERSION_1_6;
}
