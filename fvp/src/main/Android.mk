LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := droidlogic.fvp
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_DX_FLAGS := --core-library
LOCAL_DEX_PREOPT := false
LOCAL_VENDOR_MODULE := true
include $(BUILD_JAVA_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := droidlogic.fvp.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_SRC_FILES := $(LOCAL_MODULE)

LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/etc/permissions
LOCAL_VENDOR_MODULE := true

include $(BUILD_PREBUILT)

