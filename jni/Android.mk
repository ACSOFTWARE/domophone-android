LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libdomophone
LOCAL_STATIC_LIBRARIES := cpufeatures
LOCAL_LDLIBS := -llog 
LOCAL_SRC_FILES := main.c libdomophone/dconnection.c libdomophone/socketdata.c

include $(BUILD_SHARED_LIBRARY)
$(call import-module,android/cpufeatures)
