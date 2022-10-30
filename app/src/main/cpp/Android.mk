LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

DTVKITSOURCE_PLATFORM := amlogic

LOCAL_MODULE := libplatform
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
#LOCAL_SDK_VERSION := 21 # for normal app (ndk jni)
LOCAL_SHARED_LIBRARIES := liblog libandroid libnativehelper

$(info inputsource: platform '$(DTVKITSOURCE_PLATFORM)')
ifeq (broadcom,$(DTVKITSOURCE_PLATFORM))
LOCAL_C_INCLUDES += system/core/libcutils/include
LOCAL_C_INCLUDES += vendor/broadcom/bcm_platform/media/libbcmsideband/include
LOCAL_C_INCLUDES += vendor/broadcom/bcm_platform/media/libbcmsidebandplayer/include
LOCAL_SHARED_LIBRARIES += libbcmsideband
LOCAL_CFLAGS += -DPLATFORM_BROADCOM
endif
ifeq (amlogic,$(DTVKITSOURCE_PLATFORM))
LOCAL_CFLAGS += -DPLATFORM_AMLOGIC
LOCAL_C_INCLUDES += hardware/amlogic/gralloc \
    frameworks/base/include \
    frameworks/native/include

LOCAL_HEADER_LIBRARIES := jni_headers
LOCAL_SHARED_LIBRARIES += \
    libcutils \
    libutils \
    libgui \
    libnativehelper \
    libandroid_runtime \
    libui \
    libamgralloc_ext@2
endif



LOCAL_SRC_FILES := platform.cpp

#include $(BUILD_SHARED_LIBRARY)

