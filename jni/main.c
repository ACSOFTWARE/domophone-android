//============================================================================
// Name        : libdomophone - main.c
// Author      : AC SOFTWARE SP. Z O.O.
// Version     : 1.2
// Copyright   : (c) 2012-2013
//============================================================================

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <pthread.h>
#include <dlfcn.h>
#include <android/log.h>
#include <cpu-features.h>
#include "libdomophone/dconnection.h"


unsigned char key_to_buffer(JNIEnv* env, jstring key, char *buff, int buff_size) {
    
    int a, len;
    unsigned char result = 0;
    const char *nativeString = (*env)->GetStringUTFChars(env, key, 0);
    char *str = NULL;
    
    if (nativeString
        && strlen(nativeString) == buff_size*2+buff_size/2-1 ) {
        str =  strdup(nativeString);
        
    }
    
    (*env)->ReleaseStringUTFChars(env, key, nativeString);
    
    if ( str ) {
        
        len = strlen(str);
        buff_size = 0;
        result = 1;
        
        for(a=0;a<len;a+=2) {
            
            str[a] = toupper(str[a]);
            
            if ( (a+1)%5 == 0 ) {
                if ( str[a] != '-' ) {
                    result = 0;
                    break;
                } else {
                    a++;
                }
            }
            
            if ( ( str[a] >= 'A' && str[a] <= 'F')
                || ( str[a] >= '0' && str[a] <= '9' ) ) {
                str[a]-=str[a]>64 ? 55 : 48;
                str[a]*=16;
                str[a+1]-=str[a+1]>64 ? 55 : 48;
                buff[buff_size] = str[a] + str[a+1];
                buff_size++;
            } else {
                result = 0;
                break;
            }
            
        }

        
    }
    
    if ( str ) {
        free(str);
    }
    
    if ( result == 0 ) {
    //    memset(buff, 0, buff_size);
    }
    
   
    return result;
}

jstring key_to_jstring(JNIEnv* env, char *key, int key_size) {
    

    char *str = malloc(key_size*2+key_size/2+3);
    str[0] = 0;
    
    int n=0;
    int a;
    for(a=0;a<key_size;a++) {
        sprintf(&str[n], "%02X", (unsigned char)key[a]);
        n+=2;
        
        if ( (a+1)%2 == 0 && a<key_size-1 ) {
            str[n] = '-';
            n++;
        }
    };
    str[n] = 0;
    
    jstring result = (*env)->NewStringUTF(env, str);
    free(str);
    return result;
}

jint Java_com_acsoftware_android_domophone_LibDomophone_MemTest(JNIEnv* env, jobject thiz)
{
    void *v = malloc(1024);
    __android_log_print(ANDROID_LOG_DEBUG, "MEMTEST", "ptr=%i, %i, %i, %i", sizeof(v), sizeof(jint), sizeof(jlong));
};

jint
Java_com_acsoftware_android_domophone_LibDomophone_Init(JNIEnv* env, jobject thiz, jchar language, jstring authkey, jstring serial, jstring clientid, jstring name, jbyte useproxy) {
   
    const char *nativeName = (*env)->GetStringUTFChars(env, name, NULL);
  
    jint dc = 0;
    char _authKey[AUTHKEY_SIZE];
    char _serialKey[ID_SIZE];
    char _clientID[ID_SIZE];
    
    key_to_buffer(env, authkey, _authKey, AUTHKEY_SIZE);
    key_to_buffer(env, serial, _serialKey, ID_SIZE);
    key_to_buffer(env, clientid, _clientID, ID_SIZE);
    
    dc = (jint)dconnection_init(OSTYPE_ANDROID, (char)language, _authKey, _serialKey, _clientID, nativeName, useproxy);

    (*env)->ReleaseStringUTFChars(env, name, nativeName);
    
    return dc;
}

void
Java_com_acsoftware_android_domophone_LibDomophone_SetPingInterval(JNIEnv* env, jobject thiz, jint dc, jint interval) {
    return dconnection_set_ping_interval((void*)dc, interval);
}

void
Java_com_acsoftware_android_domophone_LibDomophone_Release(JNIEnv* env, jobject thiz, jint dc) {
    
    dconnection_release((void*)dc);
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_ProxyInit(JNIEnv* env, jobject thiz, jint dc) {
    return (jint)pconnection_proxyinit((void*)dc);
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_Work(JNIEnv* env, jobject thiz, jint dc) {
    return dconnection_work((void*)dc);
}

void
Java_com_acsoftware_android_domophone_LibDomophone_AppendRecvBuffer(JNIEnv* env, jobject thiz, jint dc, jbyteArray in, jint in_len) {
    jbyte *_in = (*env)->GetByteArrayElements(env, in, NULL);
    dconnection_appendrecvbuffer((void*)dc, (char*)_in, in_len);
    (*env)->ReleaseByteArrayElements(env, in, _in, 0);
}

jbyteArray
Java_com_acsoftware_android_domophone_LibDomophone_GetSentBuffer(JNIEnv* env, jobject thiz, jint dc, jint size) {
    char *result = dconnection_getsentbuffer((void*)dc, &size);
    if ( !result ) size = 0;
    
    jbyteArray ret = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env, ret, 0, size, (const jbyte*)result);

    if (result)
        free(result);
    
    return ret;
}

void
Java_com_acsoftware_android_domophone_LibDomophone_SetConnecting(JNIEnv* env, jobject thiz, jint dc) {
    dconnection_setconnecting((void*)dc);
}

jstring
Java_com_acsoftware_android_domophone_LibDomophone_GetAuthkey(JNIEnv* env, jobject thiz, jint dc) {
    
    char key[AUTHKEY_SIZE];
    dconnection_getauthkey((void*)dc, key);
    return key_to_jstring(env, key, AUTHKEY_SIZE);
}

void
Java_com_acsoftware_android_domophone_LibDomophone_SetDisconnected(JNIEnv* env, jobject thiz, jint dc, jint wait_for_reconnect) {
    
    dconnection_setdisconnected((void*)dc, wait_for_reconnect == 0 ? 0 : 1);
    
}

void
Java_com_acsoftware_android_domophone_LibDomophone_GetConnectionSettings(JNIEnv* env, jobject thiz, jint dc, jobject csettings, jobject sip) {

    jfieldID fid;
    TConnectionSettings cs;
    dconnection_getconnectionsettings((void*)dc, &cs);

    jclass jcs = (*env)->GetObjectClass(env, csettings);

    if ( jcs ) {

        fid = (*env)->GetFieldID(env, jcs, "Caps", "I");
        (*env)->SetIntField(env, csettings, fid, cs.Caps);
        fid = (*env)->GetFieldID(env, jcs, "Proxy", "I");
        (*env)->SetIntField(env, csettings, fid, cs.proxy==1 ? 1 : 0);
        fid = (*env)->GetFieldID(env, jcs, "AuthKey", "Ljava/lang/String;");
        (*env)->SetObjectField(env, csettings, fid, key_to_jstring(env, cs.AuthKey, AUTHKEY_SIZE));
        fid = (*env)->GetFieldID(env, jcs, "SerialKey", "Ljava/lang/String;");
        (*env)->SetObjectField(env, csettings, fid, key_to_jstring(env, cs.SerialKey, ID_SIZE));
    }
    
    jclass jsip = (*env)->GetObjectClass(env, sip);
    
    if ( jsip ) {
        
        fid = (*env)->GetFieldID(env, jsip, "Host", "Ljava/lang/String;");
        (*env)->SetObjectField(env, sip, fid, (*env)->NewStringUTF(env, cs.Sip.Host));

        fid = (*env)->GetFieldID(env, jsip, "Port", "I");
        (*env)->SetIntField(env, sip, fid, cs.Sip.Port);
    }
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_GetEvent(JNIEnv* env, jobject thiz, jint dc, jobject event) {
    
    jfieldID fid;
    TdEvent _event;
    unsigned char dup = 0;
    
    if ( dconnection_getevent((void*)dc, &_event, &dup) == 1 ) {
         
         jclass jevent = (*env)->GetObjectClass(env, event);
        if ( jevent ) {
            
            fid = (*env)->GetFieldID(env, jevent, "ID", "I");
            (*env)->SetIntField(env, event, fid, _event.ID);
            
            fid = (*env)->GetFieldID(env, jevent, "Type", "I");
            (*env)->SetIntField(env, event, fid, _event.Type);
            
            fid = (*env)->GetFieldID(env, jevent, "Scope", "I");
            (*env)->SetIntField(env, event, fid, _event.Scope);

            fid = (*env)->GetFieldID(env, jevent, "_Owner", "I");
            (*env)->SetIntField(env, event, fid, _event.Owner == 0 ? 0 : 1);
            
            fid = (*env)->GetFieldID(env, jevent, "Param1", "I");
            (*env)->SetIntField(env, event, fid, _event.Param1);
           
            fid = (*env)->GetFieldID(env, jevent, "SenderID", "Ljava/lang/String;");
            (*env)->SetObjectField(env, event, fid, key_to_jstring(env, _event.SenderID, ID_SIZE));
            
            if ( _event.SenderName ) {
                fid = (*env)->GetFieldID(env, jevent, "SenderName", "Ljava/lang/String;");
                (*env)->SetObjectField(env, event, fid, (*env)->NewStringUTF(env, (char*)_event.SenderName));
                free(_event.SenderName);
            }
            
            fid = (*env)->GetFieldID(env, jevent, "Duplicate", "I");
            (*env)->SetIntField(env, event, fid, dup == 0 ? 0 : 1);
           
            return 1;
        }
           
    }
    
    return 0;
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_RequestAction(JNIEnv* env, jobject thiz, jint dc, jint action, jint param2, jint param3, jint param4, jint param5) {
    
   return dconnection_request_action((void*)dc, action, param2, param3, param4, param5);
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_OpenGate(JNIEnv* env, jobject thiz, jint dc, jint num) {
   return dconnection_opengate((void*)dc, num);
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_SipConnect(JNIEnv* env, jobject thiz, jint dc, jint speaker_on, jint video) {
   return dconnection_sipconnect((void*)dc, speaker_on == 0 ? 0 : 1, video == 0 ? 0 : 1);
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_SipDisconnect(JNIEnv* env, jobject thiz, jint dc) {
    return dconnection_sipdisconnect((void*)dc);
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_IsProxy(JNIEnv* env, jobject thiz, jint dc) {
    return dconnection_isproxy((void*)dc) == 0 ? 0 : 1;
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_IsAuthorized(JNIEnv* env, jobject thiz, jint dc) {
    return dconnection_is_authorized((void*)dc) == 0 ? 0 : 1;
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_GetSysState(JNIEnv* env, jobject thiz, jint dc, jobject state) {
    
    int s = 0;
    int firmware_version = 0;
 
    jfieldID fid;

    jclass jstate = (*env)->GetObjectClass(env, state);
    if ( jstate ) {
       if ( dconnection_get_sys_state((void*)dc, &s, &firmware_version) ) {
 
          fid = (*env)->GetFieldID(env, jstate, "state", "I");
          (*env)->SetIntField(env, state, fid, s);

          fid = (*env)->GetFieldID(env, jstate, "firmware_version", "I");
          (*env)->SetIntField(env, state, fid, firmware_version);

          return 1;
       }
    }
 
    return -1;
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_GetLockEvent(JNIEnv* env, jobject thiz, jint dc, jobject event) {
    
    jfieldID fid;
    TDataPacket DP;
    dconnection_getresponse((void*)dc, &DP);
    
    jclass jevent = (*env)->GetObjectClass(env, event);
    if ( jevent ) {
        
        char *SenderName = NULL;
        char ID[ID_SIZE];
        
        dconnection_extract_name_and_id(&DP, &SenderName, ID);
        
        fid = (*env)->GetFieldID(env, jevent, "_Owner", "I");
        (*env)->SetIntField(env, event, fid, DP.Param2 == 0 ? 0 : 1);
        
        fid = (*env)->GetFieldID(env, jevent, "SenderID", "Ljava/lang/String;");
        (*env)->SetObjectField(env, event, fid, key_to_jstring(env, ID, ID_SIZE));
        
        if ( SenderName ) {
            
            fid = (*env)->GetFieldID(env, jevent, "SenderName", "Ljava/lang/String;");
            (*env)->SetObjectField(env, event, fid, (*env)->NewStringUTF(env, (char*)SenderName));
            __android_log_print(ANDROID_LOG_DEBUG, "JNI", "SenderName=%ls", SenderName);
                                
            free(SenderName);
        }
        
        return 1;
    }

    return 0;
}

void
Java_com_acsoftware_android_domophone_LibDomophone_setSpeakerOnOff(JNIEnv* env, jobject thiz, jint dc, jint on) {
    dconnection_setspeakeronoff((void*)dc, on == 0 ? 0 : 1);
}

jint
Java_com_acsoftware_android_domophone_LibDomophone_hasNeon(JNIEnv* env, jobject thiz) {
    return (android_getCpuFamily() == ANDROID_CPU_FAMILY_ARM) && (android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON) ? 1 : 0;
}

void
Java_com_acsoftware_android_domophone_LibDomophone_RegisterPushID(JNIEnv* env, jobject thiz, jint dc, jstring regid) {
    
    const char *nativeRegID = (*env)->GetStringUTFChars(env, regid, NULL);
    
    dconnection_set_push_id((void*)dc, (char*)nativeRegID, nativeRegID == NULL ? 0 : strlen(nativeRegID));
    
    (*env)->ReleaseStringUTFChars(env, regid, nativeRegID);
}

