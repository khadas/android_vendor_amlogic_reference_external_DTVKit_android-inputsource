LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
    DTVKitHidlClient.cpp


LOCAL_SHARED_LIBRARIES := \
    vendor.amlogic.hardware.dtvkitserver@1.0 \
    android.hidl.memory@1.0 \
    libbase \
    libhidlbase \
    libhidltransport \
    libhidlmemory \
    liblog \
    libcutils \
    libutils

LOCAL_C_INCLUDES += \
  system/libhidl/transport/include/hidl \
  system/libhidl/libhidlmemory/include

LOCAL_C_INCLUDES += \
   external/libcxx/include

LOCAL_CPPFLAGS += -std=c++14

LOCAL_MODULE:= libdtvkithidlclient
LOCAL_VENDOR_MODULE := true

include $(BUILD_SHARED_LIBRARY)
