//submodules/linphone/mediastreamer2/src/utils/opengles_display.c

JNIEXPORT void JNICALL Java_com_acsoftware_android_domophone_OpenGLESDisplay_init(JNIEnv * env,
                                                                jobject obj,
                                                                jint ptr,
                                                                jint width,
                                                                jint height) {
    
    Java_org_linphone_mediastream_video_display_OpenGLESDisplay_init(env, obj, ptr, width, height);
    
}

JNIEXPORT void JNICALL Java_com_acsoftware_android_domophone_OpenGLESDisplay_render(JNIEnv * env,
                                                                  jobject obj,
                                                                  jint ptr) {
    
    Java_org_linphone_mediastream_video_display_OpenGLESDisplay_render(env, obj, ptr);
}
