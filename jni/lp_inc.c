//submodules/linphone/coreapi/linphonecore_jni.cc
#define DEFAULT_EXPIRES 600


static LinphoneCoreVTable lp_vTable;
static LinphoneCore* lc = NULL;
static char _RegistrationInProgress = 0;
static int _retryCounter = 0;
static char _AudioEnabled = 0;
static char _VideoEnabled = 0;

void CallMethod(JNIEnv* env, const char *method) {
    
    jclass clazz = env->FindClass("com/acsoftware/android/domophone/LibLP");
    jmethodID m = env->GetStaticMethodID(clazz, method, "()V");
    env->CallStaticVoidMethod(clazz, m);
    
}

/*

void configurePayload(const char* type, int rate)  {
    PayloadType* pt;
    if( lc && (pt = linphone_core_find_payload_type(lc,type,rate))) {
        linphone_core_enable_payload_type(lc,pt, true);
    }
}
 
 */

extern "C"
void Java_com_acsoftware_android_domophone_LibLP_nSetAudioEnabled(JNIEnv* env
                                                                 ,jobject thiz
                                                                 ,int enabled) {
    _AudioEnabled = enabled == 0 ? 0 : 1;
    
    if ( lc ) {
        linphone_core_enable_mic(lc, enabled == 1);
    }
    
}

extern "C" void Java_com_acsoftware_android_domophone_LibLP_nforceSpeakerState(JNIEnv *env, jobject thiz, jboolean speakerOn) {
/*
	LinphoneCall *call = linphone_core_get_current_call(lc);
	if (call && call->audiostream && call->audiostream->soundread) {
		bool_t on = speakerOn;
		ms_filter_call_method(call->audiostream->soundread, MS_AUDIO_CAPTURE_FORCE_SPEAKER_STATE, &on);
	}
*/
}

extern "C"
void Java_com_acsoftware_android_domophone_LibLP_nSetVideoEnabled(JNIEnv* env
                                                                 ,jobject thiz
                                                                 ,int enabled) {
    _VideoEnabled = enabled == 0 ? 0: 1;
    
    if ( lc ) {
        linphone_core_enable_video_display(lc, enabled == 1);
        //linphone_core_enable_video_capture(lc, enabled == 1);
    }
}

static void _registration_state(LinphoneCore *lc, LinphoneProxyConfig* cfg, LinphoneRegistrationState state,const char* message) {
   
    JNIEnv *env = 0;
    if (jvm->AttachCurrentThread(&env,NULL) != 0) {
        ms_error("cannot attach VM\n");
        return;
    }
    
    switch(state) {
        case LinphoneRegistrationOk:
            _retryCounter = 0;
            _RegistrationInProgress = 0;
            CallMethod(env, "SipRegisteredEvent");
            break;
            //case LinphoneRegistrationCleared:
        case LinphoneRegistrationFailed:
            //if ( _retryCounter > 0 && lastIdent && lastHost ) {
            //   _retryCounter--;
            //    [self registerWithIdent:lastIdent host:lastHost];
            //} else {
                _RegistrationInProgress = 0;
                CallMethod(env, "SipRegistrationFailed");
            //};
            break;
        case LinphoneRegistrationProgress:
            CallMethod(env, "BeforeRegisterEvent");
            break;
        default:
            break;
            
    }
}

static void _call_state(LinphoneCore *lc, LinphoneCall* call, LinphoneCallState state,const char* message) {
    
    JNIEnv *env = 0;
    if (jvm->AttachCurrentThread(&env,NULL) != 0) {
        ms_error("cannot attach VM\n");
        return;
    }
    
    switch(state) {
        case LinphoneCallIncomingReceived:
            
            Java_com_acsoftware_android_domophone_LibLP_nSetVideoEnabled(env, NULL, _VideoEnabled ? JNI_TRUE : JNI_FALSE);
            Java_com_acsoftware_android_domophone_LibLP_nSetAudioEnabled(env, NULL, _AudioEnabled ? JNI_TRUE : JNI_FALSE);
            
            linphone_core_accept_call(lc, call);
            break;
        case LinphoneCallStreamsRunning:
            
            Java_com_acsoftware_android_domophone_LibLP_nSetAudioEnabled(env, NULL, _AudioEnabled ? JNI_TRUE : JNI_FALSE);
            CallMethod(env, "SipCallStarted");
            
            if (linphone_call_params_video_enabled(linphone_call_get_current_params(call))) {
            //    linphone_core_set_native_video_window_id(lc, (unsigned long)MainVC.videoFrame);
            //    linphone_call_set_next_video_frame_decoded_callback(callptr, showVideoView, NULL);
                CallMethod(env, "ShowVideoWindow");
            }
            break;
        case LinphoneCallError:
        case LinphoneCallEnd:
        case LinphoneCallReleased:
            CallMethod(env, "SipTerminate");
            break;
        default:
            break;
    }
    
}

extern "C" jint
Java_com_acsoftware_android_domophone_LibLP_nSetLogsOn(JNIEnv* env, jobject thiz, int on) {
    
	if (on!=0) {
		linphone_core_enable_logs_with_cb(linphone_android_ortp_log_handler);
	} else {
		linphone_core_disable_logs();
	}

return 1;
    
}

extern "C" void
Java_com_acsoftware_android_domophone_LibLP_nInitialize(JNIEnv* env, jobject thiz, int LogsOn, jstring juserConfig
                                                       ,jstring jfactoryConfig) {
    if ( lc ) return;

    const char* userConfig = juserConfig?env->GetStringUTFChars(juserConfig, NULL):NULL;
	const char* factoryConfig = jfactoryConfig?env->GetStringUTFChars(jfactoryConfig, NULL):NULL;
    
    Java_com_acsoftware_android_domophone_LibLP_nSetLogsOn(env, thiz, LogsOn);

#ifdef HAVE_X264
	libmsx264_init();
#endif
   
    memset(&lp_vTable,0,sizeof(lp_vTable));
    lp_vTable.registration_state_changed = _registration_state;
    lp_vTable.call_state_changed = _call_state;
    
    lc = linphone_core_new(	&lp_vTable
                                               ,userConfig
                                               ,factoryConfig
                                               ,NULL);
    
    linphone_core_set_video_device(lc, "StaticImage: Static picture");
    linphone_core_enable_video_display(lc, true); 
    //linphone_core_enable_video_capture(lc, true); // Bez transmisji w obu kierunkach pojawiają się problemy z NAT-em i RTP/video
    linphone_core_set_mtu(lc, 1500);

    LinphoneVideoPolicy vpol;
    vpol.automatically_initiate = true;
    vpol.automatically_accept = true;
    linphone_core_set_video_policy(lc, &vpol);
}

extern "C" void
Java_com_acsoftware_android_domophone_LibLP_nEnableEchoCancellation(JNIEnv* env, jobject thiz, jboolean enable) {

     LinphoneCall *call = linphone_core_get_current_call(lc);

     if ( call )
       linphone_call_enable_echo_cancellation(call, enable);
}

extern "C" jboolean 
Java_com_acsoftware_android_domophone_LibLP_nisEchoCancellationEnabled(JNIEnv*  env
                                                                       ,jobject  thiz) {
        LinphoneCall *call = linphone_core_get_current_call(lc);
        if ( call )
          return (jboolean)linphone_call_echo_cancellation_enabled(call);

return (jboolean)false;
}

extern "C" 
void Java_com_acsoftware_android_domophone_LibLP_nEnableEchoLimiter(JNIEnv*  env
                                                                             ,jobject  thiz
                                                                             ,jboolean enable) {
        linphone_core_enable_echo_limiter(lc,enable);
}

extern "C"
void Java_com_acsoftware_android_domophone_LibLP_nEnableConfortNoise(JNIEnv*  env
                                                                             ,jobject  thiz
                                                                             ,jboolean enable) {
        linphone_core_enable_generic_confort_noise(lc,enable);
}

extern "C" jboolean
Java_com_acsoftware_android_domophone_LibLP_nisConfortNoiseEnabled(JNIEnv*  env
                                                                       ,jobject  thiz) {
        return (jboolean)linphone_core_generic_confort_noise_enabled(lc);

}


extern "C" void
Java_com_acsoftware_android_domophone_LibLP_nIterate(JNIEnv* env, jobject thiz) {
    if ( lc ) {
        linphone_core_iterate(lc);
    }
}

extern "C" jint
Java_com_acsoftware_android_domophone_LibLP_nRegistered(JNIEnv* env, jobject thiz, jstring jIdent, jstring jHost) {
    
    jint result = 0;
    
    if ( lc ) {
        
        char* ident = jIdent?(char*)env->GetStringUTFChars(jIdent, NULL):NULL;
        char* host = jHost?(char*)env->GetStringUTFChars(jHost, NULL):NULL;
        
        int s;
        
        if ( ident ) {
            char *i = NULL;
            
            if ( ident != NULL && host != NULL ) {
                s = strlen(ident)+strlen(host)+6;
                i = (char*)malloc(s+1);
                
                snprintf(i, s, "sip:%s@%s", ident, host);
            }
            
            
            env->ReleaseStringUTFChars(jIdent, ident);
            ident = i;
        }
        
        if ( host ) {
            s = strlen(host)+7;
            char *h = (char*)malloc(s+1);
            
            snprintf(h, s, "sip:%s", host);
            
            env->ReleaseStringUTFChars(jHost, host);
            host = h;
        }
        
        
        LinphoneProxyConfig* proxy_cfg = linphone_core_get_default_proxy_config(lc);
        
        if ( proxy_cfg
            && linphone_proxy_config_is_registered(proxy_cfg) == 1 )
        {
            if ( ( ident && strlen(ident) > 0 ) || ( host && strlen(host) > 0 ) ) {
                
                const char *cident = linphone_proxy_config_get_identity(proxy_cfg);
                const char *chost = linphone_proxy_config_get_addr(proxy_cfg);
                
                __android_log_write(ANDROID_LOG_DEBUG, "LIBLP-cident", cident);
                __android_log_write(ANDROID_LOG_DEBUG, "LIBLP-ident", ident);
                __android_log_write(ANDROID_LOG_DEBUG, "LIBLP-chost", chost);
                __android_log_write(ANDROID_LOG_DEBUG, "LIBLP-host", host);
                
                if ( strcmp(cident, ident) == 0
                    && strcmp(chost, host) == 0 )
                    result = 2;
            };
            
            if ( result == 0 ) {
                result = 1;
            }
            
            if ( ident ) {
                free(ident);
            }
            
            if ( host ) {
                free(host);
            }
        };
    };
    
    return result;
    
}

extern "C" void
Java_com_acsoftware_android_domophone_LibLP_nTerminateCall(JNIEnv* env, jobject thiz) {
    
    if ( lc ) {
        linphone_core_terminate_all_calls(lc);
    };
    
};

extern "C" void
Java_com_acsoftware_android_domophone_LibLP_nUnregister(JNIEnv* env, jobject thiz) {
    
    if ( lc ) {
        LinphoneProxyConfig* proxy_cfg = linphone_core_get_default_proxy_config(lc);
        
        if ( proxy_cfg
            && linphone_proxy_config_is_registered(proxy_cfg) == 1 ) {
            linphone_proxy_config_edit(proxy_cfg);
            linphone_proxy_config_enable_register(proxy_cfg, FALSE);
            linphone_proxy_config_done(proxy_cfg);
        };
        
        linphone_core_clear_proxy_config(lc);
    }
}

extern "C" void
Java_com_acsoftware_android_domophone_LibLP_nRegister(JNIEnv* env, jobject thiz, jstring jIdent, jstring jHost) {
    
    char buffer[256] = {0};
    const char* ident = jIdent?env->GetStringUTFChars(jIdent, NULL):NULL;
    const char* host = jHost?env->GetStringUTFChars(jHost, NULL):NULL;
    
    if ( _RegistrationInProgress
         || !lc
         || !ident
         || !host
         || strlen(ident)+strlen(host) >= 256 ) return;
    
   // [self timerInitialize];
    
    if ( Java_com_acsoftware_android_domophone_LibLP_nRegistered(env, thiz, jIdent, jHost) != 2 ) {
        
        _RegistrationInProgress = 1;
        //lastIdent = Ident;
        //lastHost = Host;
        
        CallMethod(env, "BeforeRegisterEvent");
        
        
        Java_com_acsoftware_android_domophone_LibLP_nTerminateCall(env, thiz);
        Java_com_acsoftware_android_domophone_LibLP_nUnregister(env, thiz);
        
	    linphone_core_clear_all_auth_info(lc);
        linphone_core_clear_proxy_config(lc);
        
        LCSipTransports transportValue;
        linphone_core_get_sip_transports(lc, &transportValue);
        
        if (transportValue.tcp_port == 0) transportValue.tcp_port=transportValue.udp_port + transportValue.tls_port;
        transportValue.udp_port=0;
        transportValue.tls_port=0;
        linphone_core_set_sip_transports(lc, &transportValue);
        
        LinphoneProxyConfig* proxy_cfg = linphone_proxy_config_new();
        
        snprintf(buffer, 256, "sip:%s@%s", ident, host);
        
        linphone_proxy_config_set_identity(proxy_cfg, buffer);
        
        snprintf(buffer, 256, "sip:%s", host);
        
        linphone_proxy_config_set_server_addr(proxy_cfg, buffer);
        linphone_proxy_config_enable_register(proxy_cfg, true);
        linphone_proxy_config_expires(proxy_cfg, DEFAULT_EXPIRES);
        linphone_core_add_proxy_config(lc,proxy_cfg);
        linphone_core_set_default_proxy(lc,proxy_cfg);
        
        linphone_core_set_network_reachable(lc, true);
    } else {
        CallMethod(env, "SipRegisteredEvent");
    }
    
    if ( ident ) {
        env->ReleaseStringUTFChars(jIdent, ident);
    }
    
    if ( host ) {
        env->ReleaseStringUTFChars(jHost, host);
    }
    
};

extern "C" jint
Java_com_acsoftware_android_domophone_LibLP_nActiveCall(JNIEnv* env, jobject thiz) {
    if ( lc ) {
        return linphone_core_get_current_call(lc) == NULL ? 0 : 1;
    }
    return 0;
}

extern "C" jint
Java_com_acsoftware_android_domophone_LibLP_nStreamsRunning(JNIEnv* env, jobject thiz) {
    if ( lc ) {
         LinphoneCall *call = linphone_core_get_current_call(lc);
         if ( call != NULL
              && linphone_call_get_state(call) == LinphoneCallStreamsRunning ) {
            return 1;
         }
    }
    return 0;
}

extern "C"
void Java_com_acsoftware_android_domophone_LibLP_nsetVideoWindowId(JNIEnv* env
                                                                         ,jobject thiz
                                                                         ,jobject obj) {
	jobject oldWindow = (jobject) linphone_core_get_native_video_window_id(lc);
	if (obj != NULL) {
		obj = env->NewGlobalRef(obj);
	}
	linphone_core_set_native_video_window_id(lc,(void*)obj);
	if (oldWindow != NULL) {
		env->DeleteGlobalRef(oldWindow);
	}
}


extern "C"
int Java_com_acsoftware_android_domophone_LibLP_nGetAudioEnabled(JNIEnv* env
                                                                ,jobject thiz) {
    if ( Java_com_acsoftware_android_domophone_LibLP_nActiveCall(env, thiz) == 1 ) {
        if ( linphone_core_mic_enabled(lc) ) {
            return 1;
        } else {
            return 0;
        }
    }
    
    return _AudioEnabled;
}

extern "C"
int Java_com_acsoftware_android_domophone_LibLP_nGetVideoEnabled(JNIEnv* env
                                                                ,jobject thiz) {
    if ( Java_com_acsoftware_android_domophone_LibLP_nActiveCall(env, thiz) == 1 ) {
        if ( linphone_call_params_video_enabled(linphone_call_get_current_params(linphone_core_get_current_call(lc))) ) {
            return 1;
        } else {
            return 0;
        }
    }
    
    return _VideoEnabled;
}


extern "C"
void Java_com_acsoftware_android_domophone_LibLP_nClean(JNIEnv* env
                                                       ,jobject thiz) {
    _retryCounter = 0;
    _RegistrationInProgress = false;
    
    //lastIdent = nil;
    //lastHost = nil;
    
    Java_com_acsoftware_android_domophone_LibLP_nSetVideoEnabled(env, thiz, JNI_FALSE);
    Java_com_acsoftware_android_domophone_LibLP_nSetAudioEnabled(env, thiz, JNI_FALSE);
    
    Java_com_acsoftware_android_domophone_LibLP_nTerminateCall(env, thiz);
    Java_com_acsoftware_android_domophone_LibLP_nUnregister(env, thiz);
    
    if ( lc ) {
        linphone_core_set_network_reachable(lc, false);
    };
    
    //[_iterateTimer invalidate];
    //_iterateTimer = nil;
}

extern "C"
void Java_com_acsoftware_android_domophone_LibLP_nSetMicrophoneGain(JNIEnv*  env
                ,jobject  thiz
                ,jfloat gain) {
     if ( lc ) {
           linphone_core_set_mic_gain_db(lc,gain);
     }
}

extern "C"
void Java_com_acsoftware_android_domophone_LibLP_nSetPlaybackGain(JNIEnv*  env
                ,jobject  thiz
                ,jfloat gain) {
     if ( lc ) {
           linphone_core_set_playback_gain_db(lc,gain);
     }
}

extern "C" 
jlongArray Java_com_acsoftware_android_domophone_LibLP_nlistVideoPayloadTypes(JNIEnv*  env
   ,jobject  thiz
   ) {
        const MSList* codecs = linphone_core_get_video_codecs(lc);
        int codecsCount = ms_list_size(codecs);
        jlongArray jCodecs = env->NewLongArray(codecsCount);
        jlong *jInternalArray = env->GetLongArrayElements(jCodecs, NULL);

        for (int i = 0; i < codecsCount; i++ ) {
                jInternalArray[i] = (unsigned long) (codecs->data);
                codecs = codecs->next;
        }

        env->ReleaseLongArrayElements(jCodecs, jInternalArray, 0);

        return jCodecs;
}

extern "C" jstring Java_com_acsoftware_android_domophone_LibLP_nPayloadTypeToString(JNIEnv*  env, jobject  thiz, jlong ptr) {
        PayloadType* pt = (PayloadType*)ptr;
        char* value = ms_strdup_printf("[%s] clock [%i], bitrate [%i]"
                                                                        ,payload_type_get_mime(pt)
                                                                        ,payload_type_get_rate(pt)
                                                                        ,payload_type_get_bitrate(pt));
        jstring jvalue =env->NewStringUTF(value);
        ms_free(value);
        return jvalue;
}

/*
- (void) speakerOn {
    
    UInt32 audioRouteOverride = kAudioSessionOverrideAudioRoute_Speaker;
    AudioSessionSetProperty (kAudioSessionProperty_OverrideAudioRoute
                             , sizeof (audioRouteOverride)
                             , &audioRouteOverride);
    
    if ( lc ) {
        linphone_core_set_max_calls(lc, 1);
    }
}
 */


//LinphoneManager.getInstance().routeAudioToSpeaker();
//speaker.setBackgroundResource(R.drawable.speaker_on);
//LinphoneManager.getLc().enableSpeaker(isSpeakerEnabled);


