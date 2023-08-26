#include <android/log.h>
#include <vector>
#include "bluetooth_a2dp_glue.h"
#include "bluetooth.h"
#define TAG "BluetoothAppModuleNativeA2DPGlue"

#define BTAV_A2DP_SOURCE_CODEC_UNAVAILABLE ((btav_a2dp_codec_index_t) -1)

int codec_indices[BTAV_A2DP_QVA_CODEC_INDEX_SOURCE_MAX] = {};

static btav_source_callbacks_t* original_a2dp_callbacks = nullptr;

static btav_a2dp_codec_index_t stack_to_jni(btav_a2dp_codec_index_t val) {
  return (btav_a2dp_codec_index_t) codec_indices[val];
}

static btav_a2dp_codec_index_t jni_to_stack(int val) {
  for (int i = 0; i < BTAV_A2DP_QVA_CODEC_INDEX_SOURCE_MAX; i++) {
    if (codec_indices[i] == val) return (btav_a2dp_codec_index_t) i;
  }
  return BTAV_A2DP_SOURCE_CODEC_UNAVAILABLE;
}

static std::vector<btav_a2dp_codec_config_t> jni_to_stack(const std::vector<btav_a2dp_codec_config_t>& val) {
  std::vector<btav_a2dp_codec_config_t> stack;
  for (auto &it:val) {
    btav_a2dp_codec_config_t stack_config = it;
    stack_config.codec_type = jni_to_stack(stack_config.codec_type);
    if (stack_config.codec_type != BTAV_A2DP_SOURCE_CODEC_UNAVAILABLE) {
      stack.push_back(stack_config);
    }
  }
  return stack;
}

static std::vector<btav_a2dp_codec_config_t> stack_to_jni(const std::vector<btav_a2dp_codec_config_t>& val) {
  std::vector<btav_a2dp_codec_config_t> jni;
  for (auto &it:val) {
    btav_a2dp_codec_config_t jni_config = it;
    jni_config.codec_type = stack_to_jni(jni_config.codec_type);
    if (jni_config.codec_type != BTAV_A2DP_SOURCE_CODEC_UNAVAILABLE) {
      jni.push_back(jni_config);
    }
  }
  return jni;
}

static void bta2dp_connection_state_callback(const RawAddress& bd_addr, btav_connection_state_t state) {
  original_a2dp_callbacks->connection_state_cb(bd_addr, state);
}

static void bta2dp_audio_state_callback(const RawAddress& bd_addr, btav_audio_state_t state) {
    original_a2dp_callbacks->audio_state_cb(bd_addr, state);
}

static void bta2dp_audio_config_callback(
    const RawAddress& bd_addr, btav_a2dp_codec_config_t codec_config,
    std::vector<btav_a2dp_codec_config_t> codecs_local_capabilities,
    std::vector<btav_a2dp_codec_config_t> codecs_selectable_capabilities) {

  std::vector<btav_a2dp_codec_config_t> jni_local = stack_to_jni(codecs_local_capabilities);
  std::vector<btav_a2dp_codec_config_t> jni_selectable = stack_to_jni(codecs_selectable_capabilities);

  codec_config.codec_type = stack_to_jni(codec_config.codec_type);
  if (codec_config.codec_type != BTAV_A2DP_SOURCE_CODEC_UNAVAILABLE) {
    original_a2dp_callbacks->audio_config_cb(bd_addr, codec_config, jni_local, jni_selectable);
  } else {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s: Incorrect codec selected by stack, this is BAD", __func__);
  }
}

static bool bta2dp_mandatory_codec_preferred_callback(const RawAddress& bd_addr) {
  return original_a2dp_callbacks->mandatory_codec_preferred_cb(bd_addr);
}

static btav_source_callbacks_t glue_a2dp_callbacks = {
    sizeof(btav_source_callbacks_t),
    bta2dp_connection_state_callback,
    bta2dp_audio_state_callback,
    bta2dp_audio_config_callback,
    bta2dp_mandatory_codec_preferred_callback,
};

static bt_status_t glue_init_src(
    btav_source_callbacks_t* callbacks,
    int max_connected_audio_devices,
    const std::vector<btav_a2dp_codec_config_t> &codec_priorities,
    const std::vector<btav_a2dp_codec_config_t> &offload_enabled_codecs) {

  original_a2dp_callbacks = callbacks;
  std::vector<btav_a2dp_codec_config_t> jni_priorities = jni_to_stack(codec_priorities);
  std::vector<btav_a2dp_codec_config_t> jni_offload = jni_to_stack(offload_enabled_codecs);

  return original_a2dp_interface->init(&glue_a2dp_callbacks, max_connected_audio_devices, jni_priorities, jni_offload);
}

static bt_status_t glue_src_connect_sink(const RawAddress& bd_addr) {
  return original_a2dp_interface->connect(bd_addr);
}

static bt_status_t glue_src_disconnect_sink(const RawAddress& bd_addr) {
  return original_a2dp_interface->disconnect(bd_addr);
}

static bt_status_t glue_set_silence_device(const RawAddress& bd_addr, bool silence) {
  return original_a2dp_interface->set_silence_device(bd_addr, silence);
}

static bt_status_t glue_set_active_device(const RawAddress& bd_addr) {
  return original_a2dp_interface->set_active_device(bd_addr);
}

static bt_status_t glue_codec_config_src(const RawAddress& bd_addr,
    std::vector<btav_a2dp_codec_config_t> codec_preferences) {
  std::vector<btav_a2dp_codec_config_t> jni_prefs = jni_to_stack(codec_preferences);
  return original_a2dp_interface->config_codec(bd_addr, jni_prefs);
}

static void glue_cleanup_src(void) {
  original_a2dp_interface->cleanup();
}

const btav_source_interface_t glue_a2dp_interface = {
    sizeof(btav_source_interface_t),
    glue_init_src,
    glue_src_connect_sink,
    glue_src_disconnect_sink,
    glue_set_silence_device,
    glue_set_active_device,
    glue_codec_config_src,
    glue_cleanup_src,
};
const btav_source_interface_t *original_a2dp_interface = nullptr;