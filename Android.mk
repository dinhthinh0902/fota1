LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

ifeq ($(TARGET_BUILD_APPS),)
LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
else
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res
endif

LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current
LOCAL_CERTIFICATE := platform
LOCAL_DEX_PREOPT := false
LOCAL_MODULE_PATH := $(TARGET_OUT)/app
LOCAL_PACKAGE_NAME := UpdateFOTA
src_dirs := java/
LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_STATIC_ANDROID_LIBRARIES += \
    android-support-v7-appcompat \
    android-support-v4 
 LOCAL_AAPT_FLAGS := --auto-add-overlay
 LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat
 
include $(BUILD_PACKAGE)
include $(CLEAR_VARS)
include $(call all-makefiles-under,$(LOCAL_PATH))


