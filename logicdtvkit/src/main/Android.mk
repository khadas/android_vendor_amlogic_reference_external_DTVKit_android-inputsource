LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := droidlogic.dtvkit.software.core
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
LOCAL_OVERRIDES_PACKAGES := droidlogic-dtvkit
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_DX_FLAGS := --core-library
LOCAL_VENDOR_MODULE := true
include $(BUILD_JAVA_LIBRARY)

#copy xml to permissions directory
include $(CLEAR_VARS)
LOCAL_MODULE := droidlogic.dtvkit.software.core.xml
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0
LOCAL_LICENSE_CONDITIONS := notice
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_SRC_FILES := $(LOCAL_MODULE)

LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/etc/permissions
LOCAL_VENDOR_MODULE := true

include $(BUILD_PREBUILT)
