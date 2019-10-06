#
# Copyright (C) 2015 The CyanogenMod Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

<<<<<<< HEAD   (10b75d Automatic translation import)
LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v14-preference

LOCAL_STATIC_JAVA_LIBRARIES := \
    org.lineageos.platform.internal

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_USE_AAPT2 := true
=======
LOCAL_STATIC_JAVA_LIBRARIES := \
    org.lineageos.platform.sdk
>>>>>>> CHANGE (c46b18 Revert "Profiles: explicitly include android support libs")

LOCAL_MODULE_TAGS := optional

LOCAL_CERTIFICATE := platform
LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_PACKAGE_NAME := Profiles

include $(BUILD_PACKAGE)
