LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS := -D__STDC_CONSTANT_MACROS -Wno-sign-compare -Wno-switch  -DHAVE_NEON=1 \
      -mfpu=neon -mfloat-abi=softfp -fPIC -DANDROID -fexceptions
#-Wno-pointer-sign
LOCAL_CPPFLAGS := -std=c++11 -fPIC -Wall -Wextra -DLINUX -finline-functions -O3 -fno-strict-aliasing -fvisibility=hidden -fexceptions

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../ffmpeg
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../src

LOCAL_SRC_FILES := UDTClientAndRTPDecoderJNI.cpp

LOCAL_SHARED_LIBRARIES := UDT

LOCAL_LDLIBS :=-L$(NDK_PLATFORMS_ROOT)/android-19/arch-arm/usr/lib \
-L$(LOCAL_PATH)/../../ffmpeg/ -lavformat -lavcodec -lavdevice -lavfilter -lavutil -lswscale -lswresample\
-L$(SYSROOT)/usr/lib -llog -ljnigraphics -lz -ldl -lgcc -lGLESv2

LOCAL_MODULE    := UDTClientJni
include $(BUILD_SHARED_LIBRARY)