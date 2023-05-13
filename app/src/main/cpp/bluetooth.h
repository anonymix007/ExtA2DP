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
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 *
 */

#ifndef ANDROID_INCLUDE_BLUETOOTH_H
#define ANDROID_INCLUDE_BLUETOOTH_H

#include <stdbool.h>
#include <stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>

//#include "avrcp/avrcp.h"
#include "uuid.h"
//#include "bt_transport.h"
#include "raw_address.h"
typedef uint8_t tBT_TRANSPORT;
typedef uint8_t bt_hci_error_code_t;

/**
 * The Bluetooth Hardware Module ID
 */

#define BT_HARDWARE_MODULE_ID "bluetooth"
#define BT_STACK_MODULE_ID "bluetooth"

/** Bluetooth profile interface IDs */
#define BT_PROFILE_HANDSFREE_ID "handsfree"
#define BT_PROFILE_HANDSFREE_CLIENT_ID "handsfree_client"
#define BT_PROFILE_ADVANCED_AUDIO_ID "a2dp"
#define BT_PROFILE_ADVANCED_AUDIO_SINK_ID "a2dp_sink"
#define BT_PROFILE_SOCKETS_ID "socket"
#define BT_PROFILE_HIDHOST_ID "hidhost"
#define BT_PROFILE_HIDDEV_ID "hiddev"
#define BT_PROFILE_PAN_ID "pan"
#define BT_PROFILE_MAP_CLIENT_ID "map_client"
#define BT_PROFILE_SDP_CLIENT_ID "sdp"
#define BT_PROFILE_GATT_ID "gatt"
#define BT_PROFILE_AV_RC_ID "avrcp"
#define BT_PROFILE_AV_RC_CTRL_ID "avrcp_ctrl"
#define BT_PROFILE_HEARING_AID_ID "hearing_aid"
#define BT_PROFILE_HAP_CLIENT_ID "has_client"
#define BT_PROFILE_LE_AUDIO_ID "le_audio"
#define BT_KEYSTORE_ID "bluetooth_keystore"
#define BT_ACTIVITY_ATTRIBUTION_ID "activity_attribution"
#define BT_PROFILE_VC_ID "volume_control"
#define BT_PROFILE_CSIS_CLIENT_ID "csis_client"
#define BT_PROFILE_LE_AUDIO_ID "le_audio"
#define BT_PROFILE_LE_AUDIO_BROADCASTER_ID "le_audio_broadcaster"
#define BT_PROFILE_ATP_LOCATOR_ID "atp_locator"

/** Bluetooth Device Name */
typedef struct { uint8_t name[249]; } __attribute__((packed)) bt_bdname_t;

/** Bluetooth Adapter Visibility Modes*/
typedef enum {
    BT_SCAN_MODE_NONE,
    BT_SCAN_MODE_CONNECTABLE,
    BT_SCAN_MODE_CONNECTABLE_DISCOVERABLE
} bt_scan_mode_t;

/** Bluetooth Adapter State */
typedef enum { BT_STATE_OFF, BT_STATE_ON } bt_state_t;

/** Bluetooth Adapter Input Output Capabilities which determine Pairing/Security
 */
typedef enum {
    BT_IO_CAP_OUT,    /* DisplayOnly */
    BT_IO_CAP_IO,     /* DisplayYesNo */
    BT_IO_CAP_IN,     /* KeyboardOnly */
    BT_IO_CAP_NONE,   /* NoInputNoOutput */
    BT_IO_CAP_KBDISP, /* Keyboard display */
    BT_IO_CAP_MAX,
    BT_IO_CAP_UNKNOWN = 0xFF /* Unknown value */
} bt_io_cap_t;

/** Bluetooth Error Status */
/** We need to build on this */

typedef enum {
    BT_STATUS_SUCCESS = 0,
    BT_STATUS_FAIL,
    BT_STATUS_NOT_READY,
    BT_STATUS_NOMEM,
    BT_STATUS_BUSY,
    BT_STATUS_DONE, /* request already completed */
    BT_STATUS_UNSUPPORTED,
    BT_STATUS_PARM_INVALID,
    BT_STATUS_UNHANDLED,
    BT_STATUS_AUTH_FAILURE,
    BT_STATUS_RMT_DEV_DOWN,
    BT_STATUS_AUTH_REJECTED,
    BT_STATUS_JNI_ENVIRONMENT_ERROR,
    BT_STATUS_JNI_THREAD_ATTACH_ERROR,
    BT_STATUS_WAKELOCK_ERROR
} bt_status_t;

inline std::string bt_status_text(const bt_status_t& status) {
    switch (status) {
        case BT_STATUS_SUCCESS:
            return std::string("success");
        case BT_STATUS_FAIL:
            return std::string("fail");
        case BT_STATUS_NOT_READY:
            return std::string("not_ready");
        case BT_STATUS_NOMEM:
            return std::string("no_memory");
        case BT_STATUS_BUSY:
            return std::string("busy");
        case BT_STATUS_DONE:
            return std::string("already_done");
        case BT_STATUS_UNSUPPORTED:
            return std::string("unsupported");
        case BT_STATUS_PARM_INVALID:
            return std::string("parameter_invalid");
        case BT_STATUS_UNHANDLED:
            return std::string("unhandled");
        case BT_STATUS_AUTH_FAILURE:
            return std::string("failure");
        case BT_STATUS_RMT_DEV_DOWN:
            return std::string("remote_device_down");
        case BT_STATUS_AUTH_REJECTED:
            return std::string("rejected");
        case BT_STATUS_JNI_ENVIRONMENT_ERROR:
            return std::string("jni_env_error");
        case BT_STATUS_JNI_THREAD_ATTACH_ERROR:
            return std::string("jni_thread_error");
        case BT_STATUS_WAKELOCK_ERROR:
            return std::string("wakelock_error");
        default:
            return std::string("UNKNOWN");
    }
}

/** Bluetooth HCI Error Codes */
/** Corresponding to [Vol 2] Part D, "Error Codes" of Core_v5.1 specs */
typedef uint8_t bt_hci_error_code_t;

/** Bluetooth PinKey Code */
typedef struct { uint8_t pin[16]; } __attribute__((packed)) bt_pin_code_t;

typedef struct {
    uint8_t status;
    uint8_t ctrl_state;   /* stack reported state */
    uint64_t tx_time;     /* in ms */
    uint64_t rx_time;     /* in ms */
    uint64_t idle_time;   /* in ms */
    uint64_t energy_used; /* a product of mA, V and ms */
} __attribute__((packed)) bt_activity_energy_info;

typedef struct {
    int32_t app_uid;
    uint64_t tx_bytes;
    uint64_t rx_bytes;
} __attribute__((packed)) bt_uid_traffic_t;

/** Bluetooth Adapter Discovery state */
typedef enum {
    BT_DISCOVERY_STOPPED,
    BT_DISCOVERY_STARTED
} bt_discovery_state_t;

/** Bluetooth ACL connection state */
typedef enum {
    BT_ACL_STATE_CONNECTED,
    BT_ACL_STATE_DISCONNECTED
} bt_acl_state_t;

/** Bluetooth SDP service record */
typedef struct {
    bluetooth::Uuid uuid;
    uint16_t channel;
    char name[256];  // what's the maximum length
} bt_service_record_t;

/** Bluetooth Remote Version info */
typedef struct {
    int version;
    int sub_ver;
    int manufacturer;
} bt_remote_version_t;

typedef struct {
    uint16_t version_supported;
    uint8_t local_privacy_enabled;
    uint8_t max_adv_instance;
    uint8_t rpa_offload_supported;
    uint8_t max_irk_list_size;
    uint8_t max_adv_filter_supported;
    uint8_t activity_energy_info_supported;
    uint16_t scan_result_storage_size;
    uint16_t total_trackable_advertisers;
    bool extended_scan_support;
    bool debug_logging_supported;
    bool le_2m_phy_supported;
    bool le_coded_phy_supported;
    bool le_extended_advertising_supported;
    bool le_periodic_advertising_supported;
    uint16_t le_maximum_advertising_data_length;
    uint32_t dynamic_audio_buffer_supported;
    bool le_periodic_advertising_sync_transfer_sender_supported;
    bool le_connected_isochronous_stream_central_supported;
    bool le_isochronous_broadcast_supported;
    bool le_periodic_advertising_sync_transfer_recipient_supported;
} bt_local_le_features_t;

/* Stored the default/maximum/minimum buffer time for dynamic audio buffer.
 * For A2DP offload usage, the unit is millisecond.
 * For A2DP legacy usage, the unit is buffer queue size*/
typedef struct {
    uint16_t default_buffer_time;
    uint16_t maximum_buffer_time;
    uint16_t minimum_buffer_time;
} bt_dynamic_audio_buffer_type_t;

typedef struct {
    bt_dynamic_audio_buffer_type_t dab_item[32];
} bt_dynamic_audio_buffer_item_t;

/* Bluetooth Adapter and Remote Device property types */
typedef enum {
    /* Properties common to both adapter and remote device */
    /**
     * Description - Bluetooth Device Name
     * Access mode - Adapter name can be GET/SET. Remote device can be GET
     * Data type   - bt_bdname_t
     */
    BT_PROPERTY_BDNAME = 0x1,
    /**
     * Description - Bluetooth Device Address
     * Access mode - Only GET.
     * Data type   - RawAddress
     */
    BT_PROPERTY_BDADDR,
    /**
     * Description - Bluetooth Service 128-bit UUIDs
     * Access mode - Only GET.
     * Data type   - Array of bluetooth::Uuid (Array size inferred from property
     *               length).
     */
    BT_PROPERTY_UUIDS,
    /**
     * Description - Bluetooth Class of Device as found in Assigned Numbers
     * Access mode - Only GET.
     * Data type   - uint32_t.
     */
    BT_PROPERTY_CLASS_OF_DEVICE,
    /**
     * Description - Device Type - BREDR, BLE or DUAL Mode
     * Access mode - Only GET.
     * Data type   - bt_device_type_t
     */
    BT_PROPERTY_TYPE_OF_DEVICE,
    /**
     * Description - Bluetooth Service Record
     * Access mode - Only GET.
     * Data type   - bt_service_record_t
     */
    BT_PROPERTY_SERVICE_RECORD,

    /* Properties unique to adapter */
    /**
     * Description - Bluetooth Adapter scan mode
     * Access mode - GET and SET
     * Data type   - bt_scan_mode_t.
     */
    BT_PROPERTY_ADAPTER_SCAN_MODE,
    /**
     * Description - List of bonded devices
     * Access mode - Only GET.
     * Data type   - Array of RawAddress of the bonded remote devices
     *               (Array size inferred from property length).
     */
    BT_PROPERTY_ADAPTER_BONDED_DEVICES,
    /**
     * Description - Bluetooth Adapter Discoverable timeout (in seconds)
     * Access mode - GET and SET
     * Data type   - uint32_t
     */
    BT_PROPERTY_ADAPTER_DISCOVERABLE_TIMEOUT,

    /* Properties unique to remote device */
    /**
     * Description - User defined friendly name of the remote device
     * Access mode - GET and SET
     * Data type   - bt_bdname_t.
     */
    BT_PROPERTY_REMOTE_FRIENDLY_NAME,
    /**
     * Description - RSSI value of the inquired remote device
     * Access mode - Only GET.
     * Data type   - int8_t.
     */
    BT_PROPERTY_REMOTE_RSSI,
    /**
     * Description - Remote version info
     * Access mode - SET/GET.
     * Data type   - bt_remote_version_t.
     */

    BT_PROPERTY_REMOTE_VERSION_INFO,

    /**
     * Description - Local LE features
     * Access mode - GET.
     * Data type   - bt_local_le_features_t.
     */
    BT_PROPERTY_LOCAL_LE_FEATURES,

    /**
     * Description - Local Input/Output Capabilities for classic Bluetooth
     * Access mode - GET and SET
     * Data Type - bt_io_cap_t.
     */
    BT_PROPERTY_LOCAL_IO_CAPS,

    /**
     * Description - Local Input/Output Capabilities for BLE
     * Access mode - GET and SET
     * Data Type - bt_io_cap_t.
     */
    BT_PROPERTY_LOCAL_IO_CAPS_BLE,

    BT_PROPERTY_DYNAMIC_AUDIO_BUFFER,

    /**
     * Description - True if Remote is a Member of a Coordinated Set.
     * Access mode - GET.
     * Data Type - bool.
     */
    BT_PROPERTY_REMOTE_IS_COORDINATED_SET_MEMBER,

    BT_PROPERTY_REMOTE_DEVICE_TIMESTAMP = 0xFF,
} bt_property_type_t;

/** Bluetooth Adapter Property data structure */
typedef struct {
    bt_property_type_t type;
    int len;
    void* val;
} bt_property_t;

/** Represents the actual Out of Band data itself */
typedef struct bt_oob_data_s {
    // Both
    bool is_valid = false; /* Default to invalid data; force caller to verify */
    uint8_t address[7]; /* Bluetooth Device Address (6) plus Address Type (1) */
    uint8_t c[16];      /* Simple Pairing Hash C-192/256 (Classic or LE) */
    uint8_t r[16];      /* Simple Pairing Randomizer R-192/256 (Classic or LE) */
    uint8_t device_name[256]; /* Name of the device */

    // Classic
    uint8_t oob_data_length[2]; /* Classic only data Length. Value includes this
                                 in length */
    uint8_t class_of_device[2]; /* Class of Device (Classic or LE) */

    // LE
    uint8_t le_device_role;   /* Supported and preferred role of device */
    uint8_t sm_tk[16];        /* Security Manager TK Value (LE Only) */
    uint8_t le_flags;         /* LE Flags for discoverability and features */
    uint8_t le_appearance[2]; /* For the appearance of the device */
} bt_oob_data_t;

/** Bluetooth Device Type */
typedef enum {
    BT_DEVICE_DEVTYPE_BREDR = 0x1,
    BT_DEVICE_DEVTYPE_BLE,
    BT_DEVICE_DEVTYPE_DUAL
} bt_device_type_t;

/** Bluetooth Bond state */
typedef enum {
    BT_BOND_STATE_NONE,
    BT_BOND_STATE_BONDING,
    BT_BOND_STATE_BONDED
} bt_bond_state_t;

/** Bluetooth SSP Bonding Variant */
typedef enum {
    BT_SSP_VARIANT_PASSKEY_CONFIRMATION,
    BT_SSP_VARIANT_PASSKEY_ENTRY,
    BT_SSP_VARIANT_CONSENT,
    BT_SSP_VARIANT_PASSKEY_NOTIFICATION
} bt_ssp_variant_t;

#define BT_MAX_NUM_UUIDS 32

/** Bluetooth Interface callbacks */

/** Bluetooth Enable/Disable Callback. */
typedef void (*adapter_state_changed_callback)(bt_state_t state);

/** GET/SET Adapter Properties callback */
/* TODO: For the GET/SET property APIs/callbacks, we may need a session
 * identifier to associate the call with the callback. This would be needed
 * whenever more than one simultaneous instance of the same adapter_type
 * is get/set.
 *
 * If this is going to be handled in the Java framework, then we do not need
 * to manage sessions here.
 */
typedef void (*adapter_properties_callback)(bt_status_t status,
                                            int num_properties,
                                            bt_property_t* properties);

/** GET/SET Remote Device Properties callback */
/** TODO: For remote device properties, do not see a need to get/set
 * multiple properties - num_properties shall be 1
 */
typedef void (*remote_device_properties_callback)(bt_status_t status,
                                                  RawAddress* bd_addr,
                                                  int num_properties,
                                                  bt_property_t* properties);

/** New device discovered callback */
/** If EIR data is not present, then BD_NAME and RSSI shall be NULL and -1
 * respectively */
typedef void (*device_found_callback)(int num_properties,
                                      bt_property_t* properties);

/** Discovery state changed callback */
typedef void (*discovery_state_changed_callback)(bt_discovery_state_t state);

/** Bluetooth Legacy PinKey Request callback */
typedef void (*pin_request_callback)(RawAddress* remote_bd_addr,
                                     bt_bdname_t* bd_name, uint32_t cod,
                                     bool min_16_digit);

/** Bluetooth SSP Request callback - Just Works & Numeric Comparison*/
/** pass_key - Shall be 0 for BT_SSP_PAIRING_VARIANT_CONSENT &
 *  BT_SSP_PAIRING_PASSKEY_ENTRY */
/* TODO: Passkey request callback shall not be needed for devices with display
 * capability. We still need support this in the stack for completeness */
typedef void (*ssp_request_callback)(RawAddress* remote_bd_addr,
                                     bt_bdname_t* bd_name, uint32_t cod,
                                     bt_ssp_variant_t pairing_variant,
                                     uint32_t pass_key);

/** Bluetooth Bond state changed callback */
/* Invoked in response to create_bond, cancel_bond or remove_bond */
typedef void (*bond_state_changed_callback)(bt_status_t status,
                                            RawAddress* remote_bd_addr,
                                            bt_bond_state_t state,
                                            int fail_reason);

/** Bluetooth Address consolidate callback */
/* Callback to inform upper layer that these two addresses come from same
 * bluetooth device (DUAL mode) */
typedef void (*address_consolidate_callback)(RawAddress* main_bd_addr,
                                             RawAddress* secondary_bd_addr);

/** Bluetooth LE Address association callback */
/* Callback for the upper layer to associate the LE-only device's RPA to the
 * identity address */
typedef void (*le_address_associate_callback)(RawAddress* main_bd_addr,
                                              RawAddress* secondary_bd_addr);

/** Bluetooth ACL connection state changed callback */
typedef void (*acl_state_changed_callback)(bt_status_t status,
                                           RawAddress* remote_bd_addr,
                                           bt_acl_state_t state,
                                           int transport_link_type,
                                           bt_hci_error_code_t hci_reason);

/** Bluetooth link quality report callback */
typedef void (*link_quality_report_callback)(
        uint64_t timestamp, int report_id, int rssi, int snr,
        int retransmission_count, int packets_not_receive_count,
        int negative_acknowledgement_count);

/** Switch the buffer size callback */
typedef void (*switch_buffer_size_callback)(bool is_low_latency_buffer_size);

/** Switch the codec callback */
typedef void (*switch_codec_callback)(bool is_low_latency_buffer_size);

typedef enum { ASSOCIATE_JVM, DISASSOCIATE_JVM } bt_cb_thread_evt;

/** Thread Associate/Disassociate JVM Callback */
/* Callback that is invoked by the callback thread to allow upper layer to
 * attach/detach to/from the JVM */
typedef void (*callback_thread_event)(bt_cb_thread_evt evt);

/** Bluetooth Test Mode Callback */
/* Receive any HCI event from controller. Must be in DUT Mode for this callback
 * to be received */
typedef void (*dut_mode_recv_callback)(uint16_t opcode, uint8_t* buf,
                                       uint8_t len);

/* LE Test mode callbacks
 * This callback shall be invoked whenever the le_tx_test, le_rx_test or
 * le_test_end is invoked The num_packets is valid only for le_test_end command
 */
typedef void (*le_test_mode_callback)(bt_status_t status, uint16_t num_packets);

/** Callback invoked when energy details are obtained */
/* Ctrl_state-Current controller state-Active-1,scan-2,or idle-3 state as
 * defined by HCI spec. If the ctrl_state value is 0, it means the API call
 * failed Time values-In milliseconds as returned by the controller Energy
 * used-Value as returned by the controller Status-Provides the status of the
 * read_energy_info API call uid_data provides an array of bt_uid_traffic_t,
 * where the array is terminated by an element with app_uid set to -1.
 */
typedef void (*energy_info_callback)(bt_activity_energy_info* energy_info,
                                     bt_uid_traffic_t* uid_data);

/** Callback invoked when OOB data is returned from the controller */
typedef void (*generate_local_oob_data_callback)(tBT_TRANSPORT transport,
                                                 bt_oob_data_t oob_data);

/** TODO: Add callbacks for Link Up/Down and other generic
 *  notifications/callbacks */

/** Bluetooth DM callback structure. */
typedef struct {
    /** set to sizeof(bt_callbacks_t) */
    size_t size;
    adapter_state_changed_callback adapter_state_changed_cb;
    adapter_properties_callback adapter_properties_cb;
    remote_device_properties_callback remote_device_properties_cb;
    device_found_callback device_found_cb;
    discovery_state_changed_callback discovery_state_changed_cb;
    pin_request_callback pin_request_cb;
    ssp_request_callback ssp_request_cb;
    bond_state_changed_callback bond_state_changed_cb;
    address_consolidate_callback address_consolidate_cb;
    le_address_associate_callback le_address_associate_cb;
    acl_state_changed_callback acl_state_changed_cb;
    callback_thread_event thread_evt_cb;
    dut_mode_recv_callback dut_mode_recv_cb;
    le_test_mode_callback le_test_mode_cb;
    energy_info_callback energy_info_cb;
    link_quality_report_callback link_quality_report_cb;
    generate_local_oob_data_callback generate_local_oob_data_cb;
    switch_buffer_size_callback switch_buffer_size_cb;
    switch_codec_callback switch_codec_cb;
} bt_callbacks_t;

typedef struct {
    /** set to sizeof(bt_callbacks_t) */
    size_t size;
    adapter_state_changed_callback adapter_state_changed_cb;
    adapter_properties_callback adapter_properties_cb;
    remote_device_properties_callback remote_device_properties_cb;
    device_found_callback device_found_cb;
    discovery_state_changed_callback discovery_state_changed_cb;
    pin_request_callback pin_request_cb;
    ssp_request_callback ssp_request_cb;
    bond_state_changed_callback bond_state_changed_cb;
    address_consolidate_callback address_consolidate_cb;
    acl_state_changed_callback acl_state_changed_cb;
    callback_thread_event thread_evt_cb;
    dut_mode_recv_callback dut_mode_recv_cb;
    le_test_mode_callback le_test_mode_cb;
    energy_info_callback energy_info_cb;
    link_quality_report_callback link_quality_report_cb;
    generate_local_oob_data_callback generate_local_oob_data_cb;
    switch_buffer_size_callback switch_buffer_size_cb;
    switch_codec_callback switch_codec_cb;
} bt_callbacks_nubia328_t;

typedef void (*alarm_cb)(void* data);
typedef bool (*set_wake_alarm_callout)(uint64_t delay_millis, bool should_wake,
                                       alarm_cb cb, void* data);
typedef int (*acquire_wake_lock_callout)(const char* lock_name);
typedef int (*release_wake_lock_callout)(const char* lock_name);

/** The set of functions required by bluedroid to set wake alarms and
 * grab wake locks. This struct is passed into the stack through the
 * |set_os_callouts| function on |bt_interface_t|.
 */
typedef struct {
    /* set to sizeof(bt_os_callouts_t) */
    size_t size;
    set_wake_alarm_callout set_wake_alarm;
    acquire_wake_lock_callout acquire_wake_lock;
    release_wake_lock_callout release_wake_lock;
} bt_os_callouts_t;

/** NOTE: By default, no profiles are initialized at the time of init/enable.
 *  Whenever the application invokes the 'init' API of a profile, then one of
 *  the following shall occur:
 *
 *    1.) If Bluetooth is not enabled, then the Bluetooth core shall mark the
 *        profile as enabled. Subsequently, when the application invokes the
 *        Bluetooth 'enable', as part of the enable sequence the profile that
 * were marked shall be enabled by calling appropriate stack APIs. The
 *        'adapter_properties_cb' shall return the list of UUIDs of the
 *        enabled profiles.
 *
 *    2.) If Bluetooth is enabled, then the Bluetooth core shall invoke the
 * stack profile API to initialize the profile and trigger a
 *        'adapter_properties_cb' with the current list of UUIDs including the
 *        newly added profile's UUID.
 *
 *   The reverse shall occur whenever the profile 'cleanup' APIs are invoked
 */

/** Represents the standard Bluetooth DM interface. */
typedef struct {
    /** set to sizeof(bt_interface_t) */
    size_t size;
    /**
     * Opens the interface and provides the callback routines
     * to the implemenation of this interface.
     * The |start_restricted| flag inits the adapter in restricted mode. In
     * restricted mode, bonds that are created are marked as restricted in the
     * config file. These devices are deleted upon leaving restricted mode.
     * The |is_common_criteria_mode| flag inits the adapter in commom criteria
     * mode. The |config_compare_result| flag show the config checksum check
     * result if is in common criteria mode. The |init_flags| are config flags
     * that cannot change during run. The |is_atv| flag indicates whether the
     * local device is an Android TV
     */
    int (*init)(bt_callbacks_t* callbacks, bool guest_mode,
                bool is_common_criteria_mode, int config_compare_result,
                const char** init_flags, bool is_atv,
                const char* user_data_directory);

    /** Enable Bluetooth. */
    int (*enable)();

    /** Disable Bluetooth. */
    int (*disable)(void);

    /** Closes the interface. */
    void (*cleanup)(void);

    /** Get all Bluetooth Adapter properties at init */
    int (*get_adapter_properties)(void);

    /** Get Bluetooth Adapter property of 'type' */
    int (*get_adapter_property)(bt_property_type_t type);

    /** Set Bluetooth Adapter property of 'type' */
    /* Based on the type, val shall be one of
     * RawAddress or bt_bdname_t or bt_scanmode_t etc
     */
    int (*set_adapter_property)(const bt_property_t* property);

    /** Get all Remote Device properties */
    int (*get_remote_device_properties)(RawAddress* remote_addr);

    /** Get Remote Device property of 'type' */
    int (*get_remote_device_property)(RawAddress* remote_addr,
                                      bt_property_type_t type);

    /** Set Remote Device property of 'type' */
    int (*set_remote_device_property)(RawAddress* remote_addr,
                                      const bt_property_t* property);

    /** Get Remote Device's service record  for the given UUID */
    int (*get_remote_service_record)(const RawAddress& remote_addr,
                                     const bluetooth::Uuid& uuid);

    /** Start service discovery with tranport to get remote services */
    int (*get_remote_services)(RawAddress* remote_addr, int transport);

    /** Start Discovery */
    int (*start_discovery)(void);

    /** Cancel Discovery */
    int (*cancel_discovery)(void);

    /** Create Bluetooth Bonding */
    int (*create_bond)(const RawAddress* bd_addr, int transport);

    /** Create Bluetooth Bond using out of band data */
    int (*create_bond_out_of_band)(const RawAddress* bd_addr, int transport,
                                   const bt_oob_data_t* p192_data,
                                   const bt_oob_data_t* p256_data);

    /** Remove Bond */
    int (*remove_bond)(const RawAddress* bd_addr);

    /** Cancel Bond */
    int (*cancel_bond)(const RawAddress* bd_addr);

    /**
     * Get the connection status for a given remote device.
     * return value of 0 means the device is not connected,
     * non-zero return status indicates an active connection.
     */
    int (*get_connection_state)(const RawAddress* bd_addr);

    /** BT Legacy PinKey Reply */
    /** If accept==FALSE, then pin_len and pin_code shall be 0x0 */
    int (*pin_reply)(const RawAddress* bd_addr, uint8_t accept, uint8_t pin_len,
                     bt_pin_code_t* pin_code);

    /** BT SSP Reply - Just Works, Numeric Comparison and Passkey
     * passkey shall be zero for BT_SSP_VARIANT_PASSKEY_COMPARISON &
     * BT_SSP_VARIANT_CONSENT
     * For BT_SSP_VARIANT_PASSKEY_ENTRY, if accept==FALSE, then passkey
     * shall be zero */
    int (*ssp_reply)(const RawAddress* bd_addr, bt_ssp_variant_t variant,
                     uint8_t accept, uint32_t passkey);

    /** Get Bluetooth profile interface */
    const void* (*get_profile_interface)(const char* profile_id);

    /** Bluetooth Test Mode APIs - Bluetooth must be enabled for these APIs */
    /* Configure DUT Mode - Use this mode to enter/exit DUT mode */
    int (*dut_mode_configure)(uint8_t enable);

    /* Send any test HCI (vendor-specific) command to the controller. Must be in
     * DUT Mode */
    int (*dut_mode_send)(uint16_t opcode, uint8_t* buf, uint8_t len);
    /** BLE Test Mode APIs */
    /* opcode MUST be one of: LE_Receiver_Test, LE_Transmitter_Test, LE_Test_End
     */
    int (*le_test_mode)(uint16_t opcode, uint8_t* buf, uint8_t len);

    /** Sets the OS call-out functions that bluedroid needs for alarms and wake
     * locks. This should be called immediately after a successful |init|.
     */
    int (*set_os_callouts)(bt_os_callouts_t* callouts);

    /** Read Energy info details - return value indicates BT_STATUS_SUCCESS or
     * BT_STATUS_NOT_READY Success indicates that the VSC command was sent to
     * controller
     */
    int (*read_energy_info)();

    /**
     * Native support for dumpsys function
     * Function is synchronous and |fd| is owned by caller.
     * |arguments| are arguments which may affect the output, encoded as
     * UTF-8 strings.
     */
    void (*dump)(int fd, const char** arguments);

    /**
     * Native support for metrics protobuf dumping. The dumping format will be
     * raw byte array
     *
     * @param output an externally allocated string to dump serialized protobuf
     */
    void (*dumpMetrics)(std::string* output);

    /**
     * Clear /data/misc/bt_config.conf and erase all stored connections
     */
    int (*config_clear)(void);

    /**
     * Clear (reset) the dynamic portion of the device interoperability database.
     */
    void (*interop_database_clear)(void);

    /**
     * Add a new device interoperability workaround for a remote device whose
     * first |len| bytes of the its device address match |addr|.
     * NOTE: |feature| has to match an item defined in interop_feature_t
     * (interop.h).
     */
    void (*interop_database_add)(uint16_t feature, const RawAddress* addr,
                                 size_t len);

    /**
     * Get the AvrcpTarget Service interface to interact with the Avrcp Service
     */
    //bluetooth::avrcp::ServiceInterface* (*get_avrcp_service)(void);
    void* (*get_avrcp_service)(void);
    /**
     * Obfuscate Bluetooth MAC address into a PII free ID string
     *
     * @param address Bluetooth MAC address to be obfuscated
     * @return a string of uint8_t that is unique to this MAC address
     */
    std::string (*obfuscate_address)(const RawAddress& address);

    /**
     * Get an incremental id for as primary key for Bluetooth metric and log
     *
     * @param address Bluetooth MAC address of Bluetooth device
     * @return int incremental Bluetooth id
     */
    int (*get_metric_id)(const RawAddress& address);

    /**
     * Set the dynamic audio buffer size to the Controller
     */
    int (*set_dynamic_audio_buffer_size)(int codec, int size);

    /**
     * Fetches the local Out of Band data.
     */
    int (*generate_local_oob_data)(tBT_TRANSPORT transport);

    /**
     * Allow or disallow audio low latency
     *
     * @param allowed true if allowing audio low latency
     * @param address Bluetooth MAC address of Bluetooth device
     * @return true if audio low latency is successfully allowed or disallowed
     */
    bool (*allow_low_latency_audio)(bool allowed, const RawAddress& address);

    /**
     * Set the event filter for the controller
     */
    int (*clear_event_filter)();

    /**
     * Data passed from BluetoothDevice.metadata_changed
     *
     * @param remote_bd_addr remote address
     * @param key Metadata key
     * @param value Metadata value
     */
    void (*metadata_changed)(const RawAddress& remote_bd_addr, int key,
                             std::vector<uint8_t> value);

} bt_interface_t;

#define BLUETOOTH_INTERFACE_STRING "bluetoothInterface"

#endif /* ANDROID_INCLUDE_BLUETOOTH_H */