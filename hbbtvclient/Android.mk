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

else

include $(CLEAR_VARS)
include $(LOCAL_PATH)/./src/main/Android.mk

endif
