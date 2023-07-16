#ifndef EXTA2DP_BLUETOOTH_A2DP_GLUE_H
#define EXTA2DP_BLUETOOTH_A2DP_GLUE_H
#include <unordered_map>
#include "bt_av.h"

extern int codec_indices[BTAV_A2DP_QVA_CODEC_INDEX_SOURCE_MAX];

extern const btav_source_interface_t glue_a2dp_interface;
extern const btav_source_interface_t *original_a2dp_interface;

#endif //EXTA2DP_BLUETOOTH_A2DP_GLUE_H
