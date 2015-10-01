LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
APP_STL := gnustl_static
LOCAL_CFLAGS := -DLINUX -fvisibility=hidden

LOCAL_ARM_MODE := arm
LOCAL_CPPFLAGS := -fPIC -Wall -Wextra -DLINUX -finline-functions -O3 -fno-strict-aliasing -fvisibility=hidden -fexceptions
LOCAL_LDLIBS := -llog
LOCAL_CPP_EXTENSION:=.cpp

LOCAL_MODULE := UDT
LOCAL_SRC_FILES := md5.cpp \
common.cpp \
window.cpp \
list.cpp \
buffer.cpp \
packet.cpp \
channel.cpp \
queue.cpp \
ccc.cpp \
cache.cpp \
core.cpp \
epoll.cpp \
api.cpp
include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_STATIC_LIBRARY)