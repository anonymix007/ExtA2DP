#include <jni.h>
#include <pthread.h>
#include <android/log.h>

#include "bluetooth.h"
#include "bluetooth_callbacks_glue.h"
#define TAG "BluetoothAppModuleNativeCallbacksGlue"

static pthread_t sCallbackThread = 0;
static bool sHaveCallbackThread = false;

#define select_callback_general(cb,  fun, ...) ((cb.fresh->size >= sizeof(bt_callbacks_t)) ? cb.fresh->fun(__VA_ARGS__) : cb.old->fun(__VA_ARGS__))
#define callback(fun, ...) select_callback_general(original_callbacks, fun, __VA_ARGS__)


static void adapter_state_change_callback_glue(bt_state_t status) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, status %u", __func__, pthread_self(), status);
  //original_callbacks->adapter_state_changed_cb(status);
  callback(adapter_state_changed_cb, status);
}

static void adapter_properties_callback_glue(bt_status_t status, int num_properties,
                                        bt_property_t* properties) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, status %u", __func__, pthread_self(), status);
  JNIEnv *p = nullptr;
  if (getCallbackEnv) p = getCallbackEnv();
  else __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: getCallbackEnv IS NULL!", __func__);
  bool isThread = false;
  if (isCallbackThread) isThread = isCallbackThread();
  else __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: isCallbackThread IS NULL!", __func__);
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: getCallbackEnv() = %p, isCallbackThread() = %s", __func__, p, isThread ? "true":"false");
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: sHaveCallbackThread = %s, pthread_equal(sCallbackThread, pthread_self()) = %s (%lu == %lu)", __func__, sHaveCallbackThread ? "true":"false" , pthread_equal(sCallbackThread, pthread_self())? "true":"false", sCallbackThread, pthread_self());
  //original_callbacks->adapter_properties_cb(status, num_properties, properties);
  callback(adapter_properties_cb, status, num_properties, properties);
}

static void remote_device_properties_callback_glue(bt_status_t status,
                                              RawAddress* bd_addr,
                                              int num_properties,
                                              bt_property_t* properties) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, status %u", __func__, pthread_self(), status);
  //original_callbacks->remote_device_properties_cb(status, bd_addr, num_properties, properties);
  callback(remote_device_properties_cb, status, bd_addr, num_properties, properties);
}

static void device_found_callback_glue(int num_properties,
                                  bt_property_t* properties) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, num %u", __func__, pthread_self(), num_properties);
  //original_callbacks->device_found_cb(num_properties, properties);
  callback(device_found_cb, num_properties, properties);
}

static void bond_state_changed_callback_glue(bt_status_t status, RawAddress* bd_addr,
                                        bt_bond_state_t state,
                                        int fail_reason) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, status %u", __func__, pthread_self(), status);
  //original_callbacks->bond_state_changed_cb(status, bd_addr, state, fail_reason);
  callback(bond_state_changed_cb, status, bd_addr, state, fail_reason);
}

static void acl_state_changed_callback_glue(bt_status_t status, RawAddress* bd_addr,
                                       bt_acl_state_t state,
                                       int transport_link_type,
                                       bt_hci_error_code_t hci_reason) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, status %u, hci_reason %u", __func__, pthread_self(), status, hci_reason);
  //original_callbacks->acl_state_changed_cb(status, bd_addr, state, transport_link_type, hci_reason);
  callback(acl_state_changed_cb, status, bd_addr, state, transport_link_type, hci_reason);
}

static void discovery_state_changed_callback_glue(bt_discovery_state_t state) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, state %u", __func__, pthread_self(), state);
  //original_callbacks->discovery_state_changed_cb(state);
  callback(discovery_state_changed_cb, state);
}

static void pin_request_callback_glue(RawAddress* bd_addr, bt_bdname_t* bdname,
                                 uint32_t cod, bool min_16_digits) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
  //original_callbacks->pin_request_cb(bd_addr, bdname, cod, min_16_digits);
  callback(pin_request_cb, bd_addr, bdname, cod, min_16_digits);
}

static void ssp_request_callback_glue(RawAddress* bd_addr, bt_bdname_t* bdname,
                                 uint32_t cod, bt_ssp_variant_t pairing_variant,
                                 uint32_t pass_key) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
  //original_callbacks->ssp_request_cb(bd_addr, bdname, cod, pairing_variant, pass_key);
  callback(ssp_request_cb, bd_addr, bdname, cod, pairing_variant, pass_key);
}

static void generate_local_oob_data_callback_glue(tBT_TRANSPORT transport,
                                             bt_oob_data_t oob_data) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, transport %u", __func__, pthread_self(), transport);
  //original_callbacks->generate_local_oob_data_cb(transport, oob_data);
  callback(generate_local_oob_data_cb, transport, oob_data);
}

static void callback_thread_event_glue(bt_cb_thread_evt event) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu, event %u",__func__, pthread_self(), event);
  if (event == ASSOCIATE_JVM) {
    sHaveCallbackThread = true;
    sCallbackThread = pthread_self();
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Attached callback thread: sCallbackThread = %lu", sCallbackThread);
  } else if (event == DISASSOCIATE_JVM) {
    if (!isCallbackThread()) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Callback: '%s' is not called on the correct thread: expected %lu, got %lu", __func__, sCallbackThread, pthread_self());
    }
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Callback: '%s' stopped", __func__);
    sHaveCallbackThread = false;
  }
  //original_callbacks->thread_evt_cb(event);
  callback(thread_evt_cb, event);
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: original callback thread_evt_cb called!", __func__);
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: original callback size %zu, exta2dp %zu", __func__, original_callbacks.fresh->size, glue_callbacks.size);
}

static void dut_mode_recv_callback_glue(uint16_t opcode, uint8_t* buf, uint8_t len) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
  //original_callbacks->dut_mode_recv_cb(opcode, buf, len);
  callback(dut_mode_recv_cb, opcode, buf, len);
}

static void le_test_mode_recv_callback_glue(bt_status_t status,
                                       uint16_t packet_count) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
  //original_callbacks->le_test_mode_cb(status, packet_count);
  callback(le_test_mode_cb, status, packet_count);
}

static void energy_info_recv_callback_glue(bt_activity_energy_info* p_energy_info,
                                      bt_uid_traffic_t* uid_data) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
  //original_callbacks->energy_info_cb(p_energy_info, uid_data);
  callback(energy_info_cb, p_energy_info, uid_data);
}

static void link_quality_report_callback_glue(
  uint64_t timestamp, int report_id, int rssi, int snr,
  int retransmission_count, int packets_not_receive_count,
  int negative_acknowledgement_count) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
  //original_callbacks->link_quality_report_cb( timestamp, report_id, rssi, snr, retransmission_count, packets_not_receive_count, negative_acknowledgement_count);
  callback(link_quality_report_cb, timestamp, report_id, rssi, snr, retransmission_count, packets_not_receive_count, negative_acknowledgement_count);
}

static void switch_buffer_size_callback_glue(bool is_low_latency_buffer_size) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
  //original_callbacks->switch_buffer_size_cb(is_low_latency_buffer_size);
  callback(switch_buffer_size_cb, is_low_latency_buffer_size);
}

static void switch_codec_callback_glue(bool is_low_latency_buffer_size) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s: callback thread %lu", __func__, pthread_self());
    //original_callbacks->switch_buffer_size_cb(is_low_latency_buffer_size);
  callback(switch_codec_cb, is_low_latency_buffer_size);
}

bt_callbacks_t glue_callbacks = {sizeof(glue_callbacks),
                                        adapter_state_change_callback_glue,
                                        adapter_properties_callback_glue,
                                        remote_device_properties_callback_glue,
                                        device_found_callback_glue,
                                        discovery_state_changed_callback_glue,
                                        pin_request_callback_glue,
                                        ssp_request_callback_glue,
                                        bond_state_changed_callback_glue,
                                        nullptr,
                                        nullptr,
                                        acl_state_changed_callback_glue,
                                        callback_thread_event_glue,
                                        dut_mode_recv_callback_glue,
                                        le_test_mode_recv_callback_glue,
                                        energy_info_recv_callback_glue,
                                        link_quality_report_callback_glue,
                                        generate_local_oob_data_callback_glue,
                                        switch_buffer_size_callback_glue,
                                        switch_codec_callback_glue};

original_callbacks_t original_callbacks;

JNIEnv* (*getCallbackEnv)() = nullptr;
bool (*isCallbackThread)() = nullptr;