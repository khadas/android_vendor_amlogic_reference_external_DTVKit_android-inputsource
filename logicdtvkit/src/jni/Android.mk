LOCAL_PATH := $(call my-dir)

### shared library

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    org_droidlogic_dtvkit_DtvkitGlueClient.cpp

LOCAL_C_INCLUDES += frameworks/base/core/jni/include \
    system/libhidl/transport/include/hidl \
    system/libhidl/libhidlmemory/include \
    system/libhidl/libhidlcache/include \
    system/libhidl/transport/token/1.0/utils/include \
    hardware/libhardware/include \
    $(LOCAL_PATH)/../../../app/src/main/client

LOCAL_MODULE := libdtvkit_jni
LOCAL_HEADER_LIBRARIES := jni_headers
LOCAL_SHARED_LIBRARIES :=  \
    vendor.amlogic.hardware.dtvkitserver@1.0 \
    android.hidl.memory@1.0 \
    libbase \
    libhidlbase \
    libhidltransport \
    libhidlmemory \
    libcutils \
    libutils \
    liblog \
    libhardware \
    libdtvkithidlclient


LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional

LOCAL_VENDOR_MODULE := true

#LOCAL_PRIVATE_PLATFORM_APIS := true
include $(BUILD_SHARED_LIBRARY)
