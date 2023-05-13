#ifndef EXTA2DP_BT_ADV_AUDIO_H
#define EXTA2DP_BT_ADV_AUDIO_H
#include <cstring>

#define is_profile(name, profile) strcmp(name, profile)



typedef struct {
    size_t size;
} bluetooth_cc_server_interface_t;

const void * get_dummy_external_profile_interface(const char *name);

#endif //EXTA2DP_BT_ADV_AUDIO_H
