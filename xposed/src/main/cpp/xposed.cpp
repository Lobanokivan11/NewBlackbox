#include <jni.h>
#include <dobby.h>
#include <lsplant.hpp>
#include <sys/sysconf.h>
#include <sys/mman.h>
#include "Utils/elf_util.h"
#include "Utils/log.h"
#include <string>
#include <vector>
#include <unistd.h>

uintptr_t align_down(uintptr_t addr, size_t alignment) {
    return addr & ~(alignment - 1);
}

void *inlineHooker(void *targetFunc, void *replaceFunc) {
    if (!targetFunc) return nullptr;

    size_t pageSize = sysconf(_SC_PAGE_SIZE);
    void *alignedAddr = (void *)align_down((uintptr_t)targetFunc, pageSize);
    mprotect(alignedAddr, pageSize, PROT_READ | PROT_WRITE | PROT_EXEC);

    void *originalFunc = nullptr;
    if (DobbyHook(targetFunc, (dobby_dummy_func_t)replaceFunc, (dobby_dummy_func_t *)&originalFunc) == kMemoryOperationError) {
        LOGE("Dobby failed to hook at %p due to memory protection", targetFunc);
        return nullptr;
    }
    return originalFunc;
}

bool inlineUnHooker(void *originalFunc) {
    if (!originalFunc) return false;
    return DobbyDestroy(originalFunc) == 0;
}

std::string getArtPath() {
    const char* libDir = (sizeof(void*) == 8) ? "lib64" : "lib";
    std::vector<std::string> searchPaths = {
        "/apex/com.android.art/" + std::string(libDir) + "/libart.so",
        "/apex/com.android.runtime/" + std::string(libDir) + "/libart.so", // Для старых версий
        "/system/" + std::string(libDir) + "/libart.so"
    };

    for (const auto& path : searchPaths) {
        if (access(path.c_str(), R_OK) == 0) {
            return path;
        }
    }
    return "";
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;

    std::string artPath = getArtPath();
    if (artPath.empty()) return JNI_ERR;
    static auto art_img = std::unique_ptr<LSPosed::ElfImg>(new LSPosed::ElfImg(artPath.c_str()));
    
    lsplant::InitInfo initInfo {
        .inline_hooker = inlineHooker,
        .inline_unhooker = inlineUnHooker,
        .art_symbol_resolver = [](std::string_view symbol) -> void * {
            return art_img->getSymbAddress(symbol.data());
        },
        .art_symbol_prefix_resolver = [](auto symbol) -> void* {
            return art_img->getSymbPrefixFirstOffset(symbol);
        },
        .generated_class_name = "org/lsposed/lsplant/Stub" + std::to_string(getpid()), 
        .generated_source_name = "LSPlant",
        .generated_method_name = "m"
    };
    if (!lsplant::Init(env, initInfo)) {
        LOGE("LSPlant Init Failed - Android 15 compatibility check required");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
