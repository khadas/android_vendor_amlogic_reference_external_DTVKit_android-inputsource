LOCAL_PATH:= $(call my-dir)
ifeq (,$(wildcard $(LOCAL_PATH)/./src/main))

include $(CLEAR_VARS)
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE := amlogic-hbbtv-client
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := libs/com.amlogic.hbbtvclient.jar
LOCAL_UNINSTALLABLE_MODULE := true
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE := org.orbtv.orblibrary
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := libs/org.orbtv.orblibrary.jar
LOCAL_UNINSTALLABLE_MODULE := true
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE := liborg.orbtv.orblibrary.native
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
LOCAL_MULTILIB := 32
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := libs/armeabi-v7a/liborg.orbtv.orblibrary.native.so
LOCAL_MODULE_SUFFIX := .so
LOCAL_VENDOR_MODULE := true
#LOCAL_SHARED_LIBRARIES :=liblog libcutils libbase libutils
LOCAL_CHECK_ELF_FILES := false
include $(BUILD_PREBUILT)

else

ORB_VENDOR := true
ORB_HBBTV_VERSION ?= 203

include $(CLEAR_VARS)
include $(LOCAL_PATH)/orb/android/orblibrary/src/main/Android.mk \
    $(LOCAL_PATH)/orb/android/orbpolyfill/src/main/Android.mk \
    $(LOCAL_PATH)/src/main/Android.mk

endif
