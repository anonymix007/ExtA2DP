#include <jni.h>
#include <string>
#include <unordered_map>
#include <dlfcn.h>
#include <android/dlext.h>
#include <android/log.h>
#include <sys/system_properties.h>

#include "lsp_native.h"
#include "bluetooth.h"
#include "bluetooth_callbacks_glue.h"
#include "bluetooth_a2dp_glue.h"

#define TAG "BluetoothAppModuleNative"

//#define DEBUG

#define PROPERTY_BT_LIBRARY_NAME "ro.bluetooth.library_name"
#define DEFAULT_BT_LIBRARY_NAME "libbluetooth_qti.so"
#define EXTA2DP_BT_LIBRARY_NAME "libbluetooth_exta2dp.so"


static HookFunType hook_func = nullptr;

static void *(*backup_dlsym)(void *handle, const char *name);

static char bt_lib_path[PROP_VALUE_MAX] = "libbluetooth_qti.so";
static std::unordered_map<std::string, void *> name_to_handle = {};
static std::unordered_map<void *, std::string> handle_to_name = {};
static bt_interface_t *original_interface = nullptr;
static bt_interface_t *exta2dp_interface;
static void *exta2dp_handle = nullptr;

static int (*exta2dp_init)(bt_callbacks_t *callbacks, bool guest_mode,
                           bool is_common_criteria_mode, int config_compare_result,
                           const char **init_flags, bool is_atv,
                           const char *user_data_directory) = nullptr;

static const void *(*exta2dp_get_profile_interface)(const char *name) = nullptr;

#if 1
#include <execinfo.h>

static bool dummy(void) {
    void *const ret = __builtin_return_address(0);
    Dl_info info;
    dladdr(ret, &info);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Dummy method called from %s in %s!",
                        info.dli_fname, info.dli_sname);
    return true;
}

static bt_callbacks_t dummy_callbacks = {
        sizeof(bt_callbacks_t),
        (adapter_state_changed_callback) dummy,
        (adapter_properties_callback) dummy,
        (remote_device_properties_callback) dummy,
        (device_found_callback) dummy,
        (discovery_state_changed_callback) dummy,
        (pin_request_callback) dummy,
        (ssp_request_callback) dummy,
        (bond_state_changed_callback) dummy,
        (address_consolidate_callback) dummy,
        (le_address_associate_callback) dummy,
        (acl_state_changed_callback) dummy,
        (callback_thread_event) dummy,
        (dut_mode_recv_callback) dummy,
        (le_test_mode_callback) dummy,
        (energy_info_callback) dummy,
        (link_quality_report_callback) dummy,
        (generate_local_oob_data_callback) dummy,
        (switch_buffer_size_callback) dummy,
        (switch_codec_callback) dummy,
};
#endif

static int ends_with(const char *str, const char *suffix) {
    size_t str_len = strlen(str);
    size_t suffix_len = strlen(suffix);
    return (str_len >= suffix_len) &&
           (!memcmp(str + str_len - suffix_len, suffix, suffix_len));
}

#ifdef DEBUG
static void print_map(std::unordered_map<std::string, void *> map) {
    for (auto& it: map) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, " %s -> %p", it.first.c_str(), it.second);
    }
}
static void print_map(std::unordered_map<void *, std::string> map) {
    for (auto& it: map) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, " %p -> %s", it.first, it.second.c_str());
    }
}
#endif

static int glue_init(bt_callbacks_t *callbacks, bool guest_mode,
                     bool is_common_criteria_mode, int config_compare_result,
                     const char **init_flags, bool is_atv,
                     const char *user_data_directory) {
    original_callbacks.fresh = callbacks;

    if ((callbacks->size < sizeof(bt_callbacks_t))) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                            "%s: Applying fix for older callbacks (based on size: %zu < %zu)",
                            __func__, callbacks->size, sizeof(bt_callbacks_t));
    }

    //original_interface->init(&dummy_callbacks, guest_mode, is_common_criteria_mode, config_compare_result, init_flags, is_atv, user_data_directory);
    return exta2dp_init(&glue_callbacks, guest_mode, is_common_criteria_mode, config_compare_result,
                        init_flags, is_atv, user_data_directory);
};

//static const void * get_dummy_external_profile_interface(const char *name) {

//return &dummy_callbacks;
//}

static const void *glue_get_profile_interface(const char *name) {
    const void *result = exta2dp_get_profile_interface(name);
    if (original_callbacks.fresh != nullptr && result == nullptr) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: %s from bt_adv_audio is not available", __func__, name);
        //return original_interface->get_profile_interface(name);
        return nullptr;
    }

    if (!strcmp(name, BT_PROFILE_ADVANCED_AUDIO_ID)) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: Applying A2DP glue for changing ids", __func__);
        original_a2dp_interface = (const btav_source_interface_t *) result;
        return &glue_a2dp_interface;
    }
    return result;
}

#define REPLACE_BTIF

static void *fake_dlsym(void *handle, char *name) {
    bool isRTLD_DEFAULT = handle == RTLD_DEFAULT;
    bool ends_with_module = ends_with(name, "module");
    bool is_bt_interface = !strcmp(name, BLUETOOTH_INTERFACE_STRING);
    if (isRTLD_DEFAULT && ends_with_module) {
#ifndef REPLACE_BTIF
        handle = name_to_handle[bt_lib_path];
#else
        // FIXME: Always load modules from the same stack library
        // Maybe with __builtin_return_address?
        //if (strcmp())

        void *const ret = __builtin_return_address(0);
        Dl_info info;
        dladdr(ret, &info);
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "dlsym hook called from %s / %s!",
                            info.dli_fname, info.dli_sname);
        if (ends_with(info.dli_fname, bt_lib_path)) {
            __android_log_print(ANDROID_LOG_DEBUG, TAG,
                                "dlsym hook called for bt_lib_path: %s, module %s", bt_lib_path,
                                name);
            handle = name_to_handle[DEFAULT_BT_LIBRARY_NAME];
        } else if (ends_with(info.dli_fname, EXTA2DP_BT_LIBRARY_NAME)) {
            handle = exta2dp_handle;
        }
#endif
#ifdef DEBUG
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Change (null) to %p for library %s symbol %s", handle, bt_lib_path, name);
        print_map(name_to_handle);
#endif
    } else if (is_bt_interface) {
#ifdef DEBUG
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Hooking bluetooth library functions!");
#endif
        original_interface = (bt_interface_t *) backup_dlsym(handle, name);
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                            "Original library interface size %zu, exta2dp %zu",
                            original_interface->size, exta2dp_interface->size);
#ifndef REPLACE_BTIF
        exta2dp_init = original_interface->init;
        original_interface->init = glue_init;
        return original_interface;
#else
        return exta2dp_interface;
#endif
    }
#ifdef DEBUG
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "dlsym %s from %s (%p) : isRTLD_DEFAULT %d, ends_with_module: %d",
                        name, handle_to_name[handle].c_str(), handle, isRTLD_DEFAULT, ends_with_module);
#endif
    return backup_dlsym(handle, name);
}


static int property_get(const char *key, char *value, const char *default_value) {
    int len = __system_property_get(key, value);
    if (len > 0) {
        return len;
    }
    if (default_value) {
        len = strnlen(default_value, PROP_VALUE_MAX - 1);
        memcpy(value, default_value, len);
        value[len] = '\0';
    }
    return len;
}

static void on_library_loaded(const char *name, void *handle) {
#ifdef DEBUG
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Library %s loaded at %p", name, handle);
#endif
    if (ends_with(name, "libbluetooth_qti_jni.so")) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                            "Hooking libbluetooth_qti_jni... Well, kind of");
        getCallbackEnv = (JNIEnv *(*)()) dlsym(handle, "_ZN7android14getCallbackEnvEv");
        isCallbackThread = (bool (*)()) dlsym(handle, "_ZN7android16isCallbackThreadEv");
    }

    handle_to_name.insert(std::make_pair(handle, name));
    name_to_handle.insert(std::make_pair(name, handle));
}

extern "C"
[[gnu::visibility("default")]] [[gnu::used]]
void Java_ru_kirddos_exta2dp_modules_BluetoothAppModule_setCodecIds(JNIEnv *env, jobject thiz,
                                                               jintArray codecIds) {
    jsize length = 0;

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: Set codec IDs...", __func__);
    jint *codec_ids = env->GetIntArrayElements(codecIds, nullptr);

    if ((length = env->GetArrayLength(codecIds)) == BTAV_A2DP_QVA_CODEC_INDEX_SOURCE_MAX) {
        for (int i = 0; i < BTAV_A2DP_QVA_CODEC_INDEX_SOURCE_MAX; i++) {
            codec_indices[i] = codec_ids[i];
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: Setting codec %s stack ID %d to %d", __func__,
                                codec_index_to_name((btav_a2dp_codec_index_t) i).c_str(), i, codec_ids[i]);
        }
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: Setting codec IDs failed: incorrect length %d", __func__, length);
    }
    env->ReleaseIntArrayElements(codecIds, codec_ids, JNI_ABORT);
}


extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *jvm, void *) {
    JNIEnv *env = nullptr;
    jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnLoad");
    return JNI_VERSION_1_6;
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    __android_log_print(ANDROID_LOG_DEBUG, TAG,
                        "LSP native hook library loaded, API v%d, hook function %p",
                        entries->version, hook_func);
    property_get(PROPERTY_BT_LIBRARY_NAME, bt_lib_path, DEFAULT_BT_LIBRARY_NAME);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Bluetooth library name: %s", bt_lib_path);

    exta2dp_handle = dlopen(EXTA2DP_BT_LIBRARY_NAME, RTLD_NOW);
    // TODO: android_get_exported_namespace and android_dlopen_ext to drop libandroidicu.so and others
    // TODO: there's no need for that. Apparently, it was all for libxml2, which is not being used by the stack
    //android_dlopen_ext("libbluetooth_exta2dp.so", RTLD_NOW, NULL);
    if (exta2dp_handle == nullptr) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "ExtA2DP bluetooth library load failed: %s",
                            dlerror());
    }
    exta2dp_interface = (bt_interface_t *) dlsym(exta2dp_handle, BLUETOOTH_INTERFACE_STRING);
    if (exta2dp_interface == nullptr) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "ExtA2DP bluetooth interface load failed: %s",
                            dlerror());
    } else {
        exta2dp_init = exta2dp_interface->init;
        exta2dp_get_profile_interface = exta2dp_interface->get_profile_interface;
        exta2dp_interface->init = glue_init;
        exta2dp_interface->get_profile_interface = glue_get_profile_interface;
    }

    hook_func((void *) dlsym, (void *) fake_dlsym, (void **) &backup_dlsym);
    return on_library_loaded;
}