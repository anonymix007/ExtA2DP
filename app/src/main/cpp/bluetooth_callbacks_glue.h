#ifndef EXTA2DP_BLUETOOTH_CALLBACKS_GLUE_H
#define EXTA2DP_BLUETOOTH_CALLBACKS_GLUE_H

// OOB_ADDRESS_SIZE is 6 bytes address + 1 byte address type
#define OOB_ADDRESS_SIZE 7
#define OOB_C_SIZE 16
#define OOB_R_SIZE 16
#define OOB_NAME_MAX_SIZE 256
// Classic
#define OOB_DATA_LEN_SIZE 2
#define OOB_COD_SIZE 3
// LE
#define OOB_TK_SIZE 16
#define OOB_LE_FLAG_SIZE 1
#define OOB_LE_ROLE_SIZE 1
#define OOB_LE_APPEARANCE_SIZE 2

#define TRANSPORT_AUTO 0
#define TRANSPORT_BREDR 1
#define TRANSPORT_LE 2

typedef union {
    bt_callbacks_t *fresh;
    bt_callbacks_nubia328_t *old;
} original_callbacks_t;


extern original_callbacks_t original_callbacks;

extern bt_callbacks_t glue_callbacks;
extern JNIEnv* (*getCallbackEnv)();
extern bool (*isCallbackThread)();

#endif //EXTA2DP_BLUETOOTH_CALLBACKS_GLUE_H
