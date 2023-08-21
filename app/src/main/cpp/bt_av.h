/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_INCLUDE_BT_AV_H
#define ANDROID_INCLUDE_BT_AV_H

#include "bluetooth.h"
#include "raw_address.h"

#include <vector>

__BEGIN_DECLS

/* Bluetooth AV connection states */
typedef enum {
  BTAV_CONNECTION_STATE_DISCONNECTED = 0,
  BTAV_CONNECTION_STATE_CONNECTING,
  BTAV_CONNECTION_STATE_CONNECTED,
  BTAV_CONNECTION_STATE_DISCONNECTING
} btav_connection_state_t;

/* Bluetooth AV datapath states */
typedef enum {
  BTAV_AUDIO_STATE_REMOTE_SUSPEND = 0,
  BTAV_AUDIO_STATE_STOPPED,
  BTAV_AUDIO_STATE_STARTED,
} btav_audio_state_t;

/*
 * Enum values for each A2DP supported codec.
 * There should be a separate entry for each A2DP codec that is supported
 * for encoding (SRC), and for decoding purpose (SINK).
 */
typedef enum {
  BTAV_A2DP_CODEC_INDEX_SOURCE_MIN = 0,

  // Add an entry for each source codec here.
  // NOTE: The values should be same as those listed in the following file:
  //   BluetoothCodecConfig.java
  BTAV_A2DP_CODEC_INDEX_SOURCE_SBC = 0,
  BTAV_A2DP_CODEC_INDEX_SOURCE_AAC,
  BTAV_A2DP_CODEC_INDEX_SOURCE_APTX,
  BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_HD,
  BTAV_A2DP_CODEC_INDEX_SOURCE_LDAC,
  BTAV_A2DP_CODEC_INDEX_SOURCE_LC3,
  BTAV_A2DP_CODEC_INDEX_SOURCE_OPUS,
  BTAV_A2DP_CODEC_INDEX_SOURCE_MAX,
  BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_ADAPTIVE =
                                 BTAV_A2DP_CODEC_INDEX_SOURCE_MAX,
  BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_TWS,

  BTAV_A2DP_CODEC_INDEX_SOURCE_LHDCV3,
  BTAV_A2DP_CODEC_INDEX_SOURCE_LHDCV2,
  BTAV_A2DP_CODEC_INDEX_SOURCE_LHDCV5,
  BTAV_A2DP_CODEC_INDEX_SOURCE_LC3PLUS_HR,
  BTAV_A2DP_CODEC_INDEX_SOURCE_FLAC,
  BTAV_A2DP_QVA_CODEC_INDEX_SOURCE_MAX,

  BTAV_A2DP_CODEC_INDEX_SINK_MIN =
                             BTAV_A2DP_QVA_CODEC_INDEX_SOURCE_MAX,

  // Add an entry for each sink codec here
  BTAV_A2DP_CODEC_INDEX_SINK_SBC = BTAV_A2DP_CODEC_INDEX_SINK_MIN,
  BTAV_A2DP_CODEC_INDEX_SINK_AAC,
  BTAV_A2DP_CODEC_INDEX_SINK_LDAC,
  BTAV_A2DP_CODEC_INDEX_SINK_OPUS,

  BTAV_A2DP_CODEC_INDEX_SINK_MAX,

  BTAV_A2DP_CODEC_INDEX_MIN = BTAV_A2DP_CODEC_INDEX_SOURCE_MIN,
  BTAV_A2DP_CODEC_INDEX_MAX = BTAV_A2DP_CODEC_INDEX_SINK_MAX
} btav_a2dp_codec_index_t;

typedef enum {
  // Disable the codec.
  // NOTE: This value can be used only during initialization when
  // function btav_source_interface_t::init() is called.
  BTAV_A2DP_CODEC_PRIORITY_DISABLED = -1,

  // Reset the codec priority to its default value.
  BTAV_A2DP_CODEC_PRIORITY_DEFAULT = 0,

  // Highest codec priority.
  BTAV_A2DP_CODEC_PRIORITY_HIGHEST = 1000 * 1000
} btav_a2dp_codec_priority_t;

typedef enum {
  BTAV_A2DP_CODEC_SAMPLE_RATE_NONE = 0x0,
  BTAV_A2DP_CODEC_SAMPLE_RATE_44100 = 0x1 << 0,
  BTAV_A2DP_CODEC_SAMPLE_RATE_48000 = 0x1 << 1,
  BTAV_A2DP_CODEC_SAMPLE_RATE_88200 = 0x1 << 2,
  BTAV_A2DP_CODEC_SAMPLE_RATE_96000 = 0x1 << 3,
  BTAV_A2DP_CODEC_SAMPLE_RATE_176400 = 0x1 << 4,
  BTAV_A2DP_CODEC_SAMPLE_RATE_192000 = 0x1 << 5,
  BTAV_A2DP_CODEC_SAMPLE_RATE_16000 = 0x1 << 6,
  BTAV_A2DP_CODEC_SAMPLE_RATE_24000 = 0x1 << 7,
  BTAV_A2DP_CODEC_SAMPLE_RATE_32000 = 0x1 << 8,
  BTAV_A2DP_CODEC_SAMPLE_RATE_8000 = 0x1 << 9
} btav_a2dp_codec_sample_rate_t;

typedef enum {
  BTAV_A2DP_CODEC_FRAME_SIZE_NONE = 0x0,
  BTAV_A2DP_CODEC_FRAME_SIZE_20MS = 0x1 << 0,
  BTAV_A2DP_CODEC_FRAME_SIZE_15MS = 0x1 << 1,
  BTAV_A2DP_CODEC_FRAME_SIZE_10MS = 0x1 << 2,
  BTAV_A2DP_CODEC_FRAME_SIZE_75MS = 0x1 << 3,
} btav_a2dp_codec_frame_size_t;

typedef enum {
  BTAV_A2DP_CODEC_BITS_PER_SAMPLE_NONE = 0x0,
  BTAV_A2DP_CODEC_BITS_PER_SAMPLE_16 = 0x1 << 0,
  BTAV_A2DP_CODEC_BITS_PER_SAMPLE_24 = 0x1 << 1,
  BTAV_A2DP_CODEC_BITS_PER_SAMPLE_32 = 0x1 << 2
} btav_a2dp_codec_bits_per_sample_t;

typedef enum {
  BTAV_A2DP_CODEC_CHANNEL_MODE_NONE = 0x0,
  BTAV_A2DP_CODEC_CHANNEL_MODE_MONO = 0x1 << 0,
  BTAV_A2DP_CODEC_CHANNEL_MODE_STEREO = 0x1 << 1
} btav_a2dp_codec_channel_mode_t;

typedef enum {
  BTAV_A2DP_SCMST_DISABLED = 0x00,
  BTAV_A2DP_SCMST_ENABLED = 0x01
} btav_a2dp_scmst_enable_status_t;

static inline std::string codec_index_to_name(btav_a2dp_codec_index_t codec_type) {
  switch (codec_type) {
    case BTAV_A2DP_CODEC_INDEX_SOURCE_SBC:
      return "SBC";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_AAC:
      return "AAC";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_APTX:
      return "aptX";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_HD:
      return "aptX HD";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_ADAPTIVE:
      return "aptX Adaptive";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_LDAC:
      return "LDAC";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_TWS:
      return "aptX TWS";
    case BTAV_A2DP_CODEC_INDEX_SINK_SBC:
      return "SBC (Sink)";
    case BTAV_A2DP_CODEC_INDEX_SINK_AAC:
      return "AAC (Sink)";
    case BTAV_A2DP_CODEC_INDEX_SINK_LDAC:
      return "LDAC (Sink)";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_LC3:
      return "LC3";
    case BTAV_A2DP_CODEC_INDEX_SINK_OPUS:
      return "Opus (Sink)";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_OPUS:
      return "Opus";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_LHDCV2:
      return "LHDC V2";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_LHDCV3:
      return "LHDC V3";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_LHDCV5:
      return "LHDC V5";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_LC3PLUS_HR:
      return "LC3plus HR";
    case BTAV_A2DP_CODEC_INDEX_SOURCE_FLAC:
      return "FLAC";
    case BTAV_A2DP_CODEC_INDEX_MAX:
      return "Unknown(CODEC_INDEX_MAX)";
  }
}

/*
 * Structure for representing codec capability or configuration.
 * It is used for configuring A2DP codec preference, and for reporting back
 * current configuration or codec capability.
 * For codec capability, fields "sample_rate", "bits_per_sample" and
 * "channel_mode" can contain bit-masks with all supported features.
 */
typedef struct btav_a2dp_codec_config_t {
  btav_a2dp_codec_index_t codec_type;
  btav_a2dp_codec_priority_t
      codec_priority;  // Codec selection priority
                       // relative to other codecs: larger value
                       // means higher priority. If 0, reset to
                       // default.
  btav_a2dp_codec_sample_rate_t sample_rate;
  btav_a2dp_codec_bits_per_sample_t bits_per_sample;
  btav_a2dp_codec_channel_mode_t channel_mode;
  int64_t codec_specific_1;  // Codec-specific value 1
  int64_t codec_specific_2;  // Codec-specific value 2
  int64_t codec_specific_3;  // Codec-specific value 3
  int64_t codec_specific_4;  // Codec-specific value 4

  std::string ToString() const {
    std::string codec_name_str = codec_index_to_name(codec_type);

    std::string sample_rate_str;
    AppendCapability(sample_rate_str,
                     (sample_rate == BTAV_A2DP_CODEC_SAMPLE_RATE_NONE), "NONE");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_44100),
                     "44100");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_48000),
                     "48000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_88200),
                     "88200");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_96000),
                     "96000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_176400),
                     "176400");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_192000),
                     "192000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_16000),
                     "16000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BTAV_A2DP_CODEC_SAMPLE_RATE_24000),
                     "24000");

    std::string bits_per_sample_str;
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample == BTAV_A2DP_CODEC_BITS_PER_SAMPLE_NONE),
                     "NONE");
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample & BTAV_A2DP_CODEC_BITS_PER_SAMPLE_16),
                     "16");
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample & BTAV_A2DP_CODEC_BITS_PER_SAMPLE_24),
                     "24");
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample & BTAV_A2DP_CODEC_BITS_PER_SAMPLE_32),
                     "32");

    std::string channel_mode_str;
    AppendCapability(channel_mode_str,
                     (channel_mode == BTAV_A2DP_CODEC_CHANNEL_MODE_NONE),
                     "NONE");
    AppendCapability(channel_mode_str,
                     (channel_mode & BTAV_A2DP_CODEC_CHANNEL_MODE_MONO),
                     "MONO");
    AppendCapability(channel_mode_str,
                     (channel_mode & BTAV_A2DP_CODEC_CHANNEL_MODE_STEREO),
                     "STEREO");

    return "codec: " + codec_name_str +
           " priority: " + std::to_string(codec_priority) +
           " sample_rate: " + sample_rate_str +
           " bits_per_sample: " + bits_per_sample_str +
           " channel_mode: " + channel_mode_str +
           " codec_specific_1: " + std::to_string(codec_specific_1) +
           " codec_specific_2: " + std::to_string(codec_specific_2) +
           " codec_specific_3: " + std::to_string(codec_specific_3) +
           " codec_specific_4: " + std::to_string(codec_specific_4);
  }

 private:
  static std::string AppendCapability(std::string& result, bool append,
                                      const std::string& name) {
    if (!append) return result;
    if (!result.empty()) result += "|";
    result += name;
    return result;
  }
} btav_a2dp_codec_config_t;

typedef struct {
  btav_a2dp_scmst_enable_status_t enable_status;
  uint8_t cp_header;
} btav_a2dp_scmst_info_t;

/** Callback for connection state change.
 *  state will have one of the values from btav_connection_state_t
 */
typedef void (*btav_connection_state_callback)(const RawAddress& bd_addr,
                                               btav_connection_state_t state);

/** Callback for audiopath state change.
 *  state will have one of the values from btav_audio_state_t
 */
typedef void (*btav_audio_state_callback)(const RawAddress& bd_addr,
                                          btav_audio_state_t state);

/** Callback for audio configuration change.
 *  Used only for the A2DP Source interface.
 */
typedef void (*btav_audio_source_config_callback)(
    const RawAddress& bd_addr, btav_a2dp_codec_config_t codec_config,
    std::vector<btav_a2dp_codec_config_t> codecs_local_capabilities,
    std::vector<btav_a2dp_codec_config_t> codecs_selectable_capabilities);

/** Callback for audio configuration change.
 *  Used only for the A2DP Sink interface.
 *  sample_rate: sample rate in Hz
 *  channel_count: number of channels (1 for mono, 2 for stereo)
 */
typedef void (*btav_audio_sink_config_callback)(const RawAddress& bd_addr,
                                                uint32_t sample_rate,
                                                uint8_t channel_count);

/** Callback for querying whether the mandatory codec is more preferred.
 *  Used only for the A2DP Source interface.
 *  Return true if optional codecs are not preferred.
 */
typedef bool (*btav_mandatory_codec_preferred_callback)(
    const RawAddress& bd_addr);

/** BT-AV A2DP Source callback structure. */
typedef struct {
  /** set to sizeof(btav_source_callbacks_t) */
  size_t size;
  btav_connection_state_callback connection_state_cb;
  btav_audio_state_callback audio_state_cb;
  btav_audio_source_config_callback audio_config_cb;
  btav_mandatory_codec_preferred_callback mandatory_codec_preferred_cb;
} btav_source_callbacks_t;

/** BT-AV A2DP Sink callback structure. */
typedef struct {
  /** set to sizeof(btav_sink_callbacks_t) */
  size_t size;
  btav_connection_state_callback connection_state_cb;
  btav_audio_state_callback audio_state_cb;
  btav_audio_sink_config_callback audio_config_cb;
} btav_sink_callbacks_t;

/**
 * NOTE:
 *
 * 1. AVRCP 1.0 shall be supported initially. AVRCP passthrough commands
 *    shall be handled internally via uinput
 *
 * 2. A2DP data path shall be handled via a socket pipe between the AudioFlinger
 *    android_audio_hw library and the Bluetooth stack.
 *
 */

/** Represents the standard BT-AV A2DP Source interface.
 */
typedef struct {
  /** set to sizeof(btav_source_interface_t) */
  size_t size;
  /**
   * Register the BtAv callbacks.
   */
  bt_status_t (*init)(
      btav_source_callbacks_t* callbacks, int max_connected_audio_devices,
      const std::vector<btav_a2dp_codec_config_t>& codec_priorities,
      const std::vector<btav_a2dp_codec_config_t>& offloading_preference);

  /** connect to headset */
  bt_status_t (*connect)(const RawAddress& bd_addr);

  /** dis-connect from headset */
  bt_status_t (*disconnect)(const RawAddress& bd_addr);

  /** sets the connected device silence state */
  bt_status_t (*set_silence_device)(const RawAddress& bd_addr, bool silence);

  /** sets the connected device as active */
  bt_status_t (*set_active_device)(const RawAddress& bd_addr);

  /** configure the codecs settings preferences */
  bt_status_t (*config_codec)(
      const RawAddress& bd_addr,
      std::vector<btav_a2dp_codec_config_t> codec_preferences);

  /** Closes the interface. */
  void (*cleanup)(void);
} btav_source_interface_t;

/** Represents the standard BT-AV A2DP Sink interface.
 */
typedef struct {
  /** set to sizeof(btav_sink_interface_t) */
  size_t size;
  /**
   * Register the BtAv callbacks
   */
  bt_status_t (*init)(btav_sink_callbacks_t* callbacks,
                      int max_connected_audio_devices);

  /** connect to headset */
  bt_status_t (*connect)(const RawAddress& bd_addr);

  /** dis-connect from headset */
  bt_status_t (*disconnect)(const RawAddress& bd_addr);

  /** Closes the interface. */
  void (*cleanup)(void);

  /** Sends Audio Focus State. */
  void (*set_audio_focus_state)(int focus_state);

  /** Sets the audio track gain. */
  void (*set_audio_track_gain)(float gain);

  /** sets the connected device as active */
  bt_status_t (*set_active_device)(const RawAddress& bd_addr);
} btav_sink_interface_t;

__END_DECLS

#endif /* ANDROID_INCLUDE_BT_AV_H */
