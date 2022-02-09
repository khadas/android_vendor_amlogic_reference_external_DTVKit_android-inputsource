LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := inputsource

LOCAL_AAPT_FLAGS := --auto-add-overlay \
   --extra-packages android.support.v17.leanback

LOCAL_RESOURCE_DIR := frameworks/support/leanback/src/main/res \
   $(LOCAL_PATH)/res \
   $(LOCAL_PATH)/droidlogic/res

LOCAL_STATIC_JAVA_LIBRARIES += \
   guava \
   companionlibrary \
   android-support-v17-leanback \
   libvewdcore-shared-hbbtv \
   libvewdcore-client-hbbtv

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/aidl

LOCAL_SRC_FILES := $(call all-subdir-java-files) $(call all-subdir-Iaidl-files)

#TARGET_BUILD_APPS := inputsource # for normal app (embedded ndk jni)
#LOCAL_JNI_SHARED_LIBRARIES := libplatform
#LOCAL_REQUIRED_MODULES := libplatform
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_JNI_SHARED_LIBRARIES := libdtvkit_jni

ifeq (1, $(strip $(shell expr $(PLATFORM_VERSION) \< 12)))
LOCAL_JAVA_LIBRARIES += droidlogic droidlogic.dtvkit.software.core
LOCAL_REQUIRED_MODULES := droidlogic droidlogic.dtvkit.software.core
else
LOCAL_REQUIRED_MODULES := droidlogic.software.core droidlogic.dtvkit.software.core
LOCAL_JAVA_LIBRARIES += droidlogic.software.core droidlogic.dtvkit.software.core
LOCAL_USES_LIBRARIES := droidlogic.software.core droidlogic.dtvkit.software.core
LOCAL_FULL_LIBS_MANIFEST_FILES := \
  $(LOCAL_PATH)/AndroidManifest.xml \
  $(LOCAL_PATH)/AndroidManifest-alarm-permissions.xml
endif
LOCAL_DEX_PREOPT := false
LOCAL_VENDOR_MODULE := true

#LOCAL_PRIVATE_PLATFORM_APIS := true
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE := libvewdcore-shared-hbbtv
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := hbbtv/libs/vewdcore-shared.jar
LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_PREBUILT)

include $(CLEAR_VARS)

LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE := libvewdcore-client-hbbtv
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := hbbtv/libs/vewdcore-client.jar
LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_PREBUILT)

include $(call all-makefiles-under, $(LOCAL_PATH))

