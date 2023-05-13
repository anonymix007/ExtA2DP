#include <cstring>
#include "bt_adv_audio.h"


static void *btif_pacs_client_get_interface(void) {
    return nullptr;
}

static void *btif_apm_get_interface(void) {
    return nullptr;
}

static void *btif_acm_initiator_get_interface(void) {
    return nullptr;
}

static void *btif_bap_broadcast_get_interface(void) {
    return nullptr;
}

static void *btif_csip_get_interface(void) {
    return nullptr;
}

static void *btif_vcp_get_controller_interface(void) {
    return nullptr;
}

static void *btif_mcp_server_get_interface(void) {
    return nullptr;
}

static bluetooth_cc_server_interface_t cc_server = {
        sizeof(bluetooth_cc_server_interface_t),
};

static void *btif_ascs_client_get_interface(void) {
    return nullptr;
}

static void *btif_bap_uclient_get_interface(void) {
    return nullptr;
}


const void * get_dummy_external_profile_interface(const char *name) {
    if (is_profile(name,"bt_pacs_client")) {
        return btif_pacs_client_get_interface();
    }
    if (is_profile(name,"apm")) {
        return btif_apm_get_interface();
    }
    if (is_profile(name,"bt_acm_proflie")) {
        return btif_acm_initiator_get_interface();
    }
    if (is_profile(name,"bap_broadcast")) {
        return btif_bap_broadcast_get_interface();
    }
    if (is_profile(name,"csip_client")) {
        return btif_csip_get_interface();
    }
    if (is_profile(name,"volume_control")) {
        return btif_vcp_get_controller_interface();
    }
    if (is_profile(name,"mcs_server")) {
        return btif_mcp_server_get_interface();
    }
    if (is_profile(name,"cc_server")) {
        return nullptr;//&cc_server;
    }
    if (is_profile(name,"bt_ascs_client")) {
        return btif_ascs_client_get_interface();
    }
    if (is_profile(name,"bt_bap_uclient")) {
        return btif_bap_uclient_get_interface();
    }
    return (void *)0x0;
}
