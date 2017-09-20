package com.acsoftware.android.domophone;
/*
 Copyright (C) AC SOFTWARE SP. Z O.O.
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.google.android.gms.wallet.EnableWalletOptimizationReceiver;

import android.content.IntentSender.SendIntentException;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;

public class LibLP {
	
	
	public static final int LPEVENT_BEFORE_REGISTER         = 1;
	public static final int LPEVENT_REGISTERED              = 2;
	public static final int LPEVENT_DO_TERMINATE            = 3;
	public static final int LPEVENT_CALLSTARTED             = 4;
	public static final int LPEVENT_SHOW_VIDEO_WINDOW       = 5;
	public static final int LPEVENT_SIP_REGISTRATION_FAILED = 6;
	
	private static final int buildVersion = Build.VERSION.SDK_INT;
	private static Handler mHandler;
	
	private native void nSetLogsOn(int On);
	private native void nInitialize(int LogsOn, String userConfig, String factoryConfig);
	private native int nRegistered(String Ident, String Host);
	private native void nTerminateCall(); 
	private native void nUnregister();
	private native void nRegister(String Ident, String Host);
	private native void nIterate();
	private native int nActiveCall();
	private native int nStreamsRunning(); 
	private native void nsetVideoWindowId(Object wid);
	private native void nClean();
	private native void nSipDisconnect();	
	private native void nSetVideoEnabled(int Enabled);
	private native void nSetAudioEnabled(int Mute);
	private native void nforceSpeakerState(boolean SpeakerOn);
	private native int nGetVideoEnabled();
	private native int nGetAudioEnabled();
	private native void nSetMicrophoneGain(float gain);
	private native void nSetPlaybackGain(float gain);
	private native long[] nlistVideoPayloadTypes();
	private native String nPayloadTypeToString(long ptr);
	private native void nEnableEchoCancellation(boolean Enabled);
	private native void nEnableEchoLimiter(boolean Enabled);
	private native boolean nisEchoCancellationEnabled();
	private native void nEnableConfortNoise(boolean Enable);
	private native boolean nisConfortNoiseEnabled();
	
	public synchronized void SetLogsOn(boolean On) {
		nSetLogsOn(On==true ? 1 : 0);
	}
	
	public synchronized void Initialize(boolean LogsOn, String userConfig, String factoryConfig) {
		nInitialize(LogsOn == true ? 1 : 0, userConfig, factoryConfig);
	}
	
	public synchronized int Registered(String Ident, String Host) {
		return nRegistered(Ident, Host);
	}
	
	public synchronized void TerminateCall() {
		nTerminateCall();
	}
	
	public synchronized void Unregister() {
		nUnregister();
	}
	
	public synchronized void Register(String Ident, String Host) {
		nRegister(Ident, Host);
	}
	
	public synchronized void Iterate() {
		nIterate();
	}
	
	public synchronized boolean ActiveCall() {
		return nActiveCall() == 1;
	}
	
	public synchronized boolean StreamsRunning() {
		return nStreamsRunning() == 1;
	}
	
	public synchronized void setVideoWindowId(Object wid) {
		nsetVideoWindowId(wid);
	}
	
	public synchronized void Clean() {
		nClean();
	}
	
	public synchronized void SipDisconnect() {
		nSipDisconnect();
	}
	
	public synchronized void SetVideoEnabled(boolean Enabled) {
		nSetVideoEnabled(Enabled==true ? 1: 0);
	}
	
	public synchronized void SetAudioEnabled(boolean Mute) {
		nSetAudioEnabled(Mute==true ? 1: 0);
	}
	
	public synchronized void forceSpeakerState(boolean SpeakerOn) {
		nforceSpeakerState(SpeakerOn);
	}
	
	public synchronized boolean GetVideoEnabled() {
		return nGetVideoEnabled() == 1 ? true : false;
	}
	
	public synchronized boolean GetAudioEnabled() {
		return nGetAudioEnabled() == 1 ? true : false;
	}
	
	public synchronized void SetMicrophoneGain(float gain) {
		nSetMicrophoneGain(gain);
	}
	
	public synchronized void SetPlaybackGain(float gain) {
		nSetPlaybackGain(gain);
	}
	
	public synchronized long[] listVideoPayloadTypes() {
		return nlistVideoPayloadTypes();
	}
	
	public synchronized String PayloadTypeToString(long ptr) {
		return nPayloadTypeToString(ptr);
	}
	
	private static boolean loadOptionalLibrary(String s) {
		try {
			System.loadLibrary(s);
			return true;
		} catch (Throwable e) {
			Trace.w("Unable to load optional library lib", s);
		}
		return false;
	}
	
    private static void sendMessage(int What) {
    	sendMessage(What, null);
    }
    
    private static void sendMessage(int What, Object obj) {
    	if ( mHandler != null ) {
        	mHandler.sendMessage(mHandler.obtainMessage(What, obj));
    	}
    }
	public LibLP(Handler _mHandler) {
		mHandler = _mHandler;
	}
	
	public static void BeforeRegisterEvent() {
	//	sendMessage(LPEVENT_BEFORE_REGISTER);
	}
	
	public static void SipRegisteredEvent() {
		sendMessage(LPEVENT_REGISTERED);
	}
	
	public static void SipTerminate() {
		sendMessage(LPEVENT_DO_TERMINATE);
	}
	
	public static void SipRegistrationFailed() {
		sendMessage(LPEVENT_SIP_REGISTRATION_FAILED);
	}
	
	public static void SipCallStarted() {
		sendMessage(LPEVENT_CALLSTARTED);
	}
	
	public static void ShowVideoWindow() {
		sendMessage(LPEVENT_SHOW_VIDEO_WINDOW);
	}
	

	public static final boolean sdkAboveOrEqual(int value) {
         return buildVersion >= value;
    }
	
	
	 public synchronized String[] getVideoCodecs() {
         long[] typesPtr = listVideoPayloadTypes();
         if (typesPtr == null) return null;

         String[] codecs = new String[typesPtr.length];

         for (int i=0; i < codecs.length; i++) {
                 codecs[i] = PayloadTypeToString(typesPtr[i]);
         }

         return codecs;
     }
	 
	 
		public synchronized void EnableEchoLimiter(boolean Enabled) {
			nEnableEchoLimiter(Enabled);
		}
	 
	 
		public synchronized void EnableEchoCancellation(boolean Enabled) {
			nEnableEchoCancellation(Enabled);
		}
		
		public synchronized boolean isEchoCancellationEnabled() {
			return nisEchoCancellationEnabled();
		}
		

		public synchronized void EnableConfortNoise(boolean Enable) {
			nEnableConfortNoise(Enable);
		}
		
		public synchronized boolean isConfortNoiseEnabled() {
			return nisConfortNoiseEnabled();
		}
	
    static {       
    	 System.loadLibrary("ffmpeg-arm");
    	 System.loadLibrary("lp");
    }
    
}
