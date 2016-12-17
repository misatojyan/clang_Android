# Copyright (C) 2009 The Android Open Source Project
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

# load static libs
include $(CLEAR_VARS)
LOCAL_MODULE := clang
LOCAL_SRC_FILES := prebuilt/libclang.so
include $(PREBUILT_SHARED_LIBRARY)

#####################################################

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp .cc
LOCAL_MODULE    := main

# the path of header files
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include

# src files
LOCAL_SRC_FILES :=  $(subst $(LOCAL_PATH)/,, \
					$(wildcard $(LOCAL_PATH)/main.cpp))
					
LOCAL_CPPFLAGS  +=  -O2 -frtti -fexceptions -std=c++11
LOCAL_CFLAGS    +=  -O2 -Wall -DANDROID 
LOCAL_LDLIBS    :=  -llog

# local static libs
LOCAL_SHARED_LIBRARIES := clang

ifeq ($(TARGET_ARCH_ABI),x86)
    LOCAL_CFLAGS += -ffast-math -mtune=atom -mssse3 -mfpmath=sse
endif

include $(BUILD_SHARED_LIBRARY)
