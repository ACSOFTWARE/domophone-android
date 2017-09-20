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

import android.R.bool;
import android.util.Log;

public class LibDomophone {
	
	public static final int AUTHKEY_SIZE                 = 8;
	public static final int ID_SIZE                      = 8;
	
	public class SipData {
		String Host;
		int Port;
	}
	
	public class ConnectionSettings {
		
		SipData Sip;
		int Caps;
		String AuthKey; 
		String SerialKey;
		int Proxy;

		public ConnectionSettings() {
			Sip = new SipData();
		}
	}

	public class dEvent {
	   int ID;
	   int Type;
	   int Scope;
	   int _Owner;
	   int Param1;
	   String SenderID;
	   String SenderName;
	   int Duplicate;
	}
	
	public class dLockEvent {
		String SenderName;
		String SenderID;
		int _Owner;
	}
	
	public class dSysState {
		int state;
		int firmware_version;
		boolean _proxy_src;
	}
	
	public static final int WRESULT_NONE                = 0;
	public static final int WRESULT_ONCONNECT           = 1;
	public static final int WRESULT_ONDISCONNECT        = 2;
	public static final int WRESULT_ONAUTHORIZE         = 3;
	public static final int WRESULT_ONUNAUTHORIZE       = 4;
	public static final int WRESULT_ONEVENT             = 5;
	public static final int WRESULT_TRYCONNECT          = 6;
	public static final int WRESULT_WAITFORDATA         = 7;
	public static final int WRESULT_TRYSENDDATA         = 8;
	public static final int WRESULT_TRYDISCONNECT       = 9;
	public static final int WRESULT_RESPONSETIMEOUT     = 10;
	public static final int WRESULT_ONRESPONSE          = 11;
	public static final int WRESULT_SYNCHMODE           = 12;
	public static final int WRESULT_PROXYCONNECT        = 13;
	public static final int WRESULT_PROXYDISCONNECT     = 14;
	public static final int WRESULT_VERSIONERROR        = 15;
	public static final int WRESULT_DEVICENOTFOUND      = 16;
	public static final int WRESULT_ONSYSSTATE          = 17;
	public static final int WRESULT_REGISTER_PUSH_ID    = 18;
	public static final int WRESULT_LOCKED              = 19;
	
	public static final byte USEPROXY_NONE               = 0;
	public static final byte USEPROXY_INSTANT            = 1;
	public static final byte USEPROXY_ALWAYS             = 2;
	
	public static final int ET_ONBEGIN                   = 1;
	public static final int ET_ONEND                     = 2;

	public static final int ES_SIP                       = 1;
	public static final int ES_OPEN                      = 2;
	public static final int ES_CLOSE                     = 3;
	public static final int ES_STOP                      = 4;
	public static final int ES_RING                      = 5;
	
	
	public static final int CAP_AUDIO      =  0x001;
	public static final int CAP_VIDEO      =  0x002;
	public static final int CAP_OPEN1      =  0x004;
	public static final int CAP_OPEN2      =  0x008;
	public static final int CAP_STOP2      =  0x010;
	public static final int CAP_CLOSE2     =  0x020;
	public static final int CAP_OPEN3      =  0x040;
	public static final int CAP_STOP3      =  0x080;
	public static final int CAP_CLOSE3     =  0x100;
	public static final int CAP_GATESENSOR    =  0x0200;
	public static final int CAP_GATEWAYSENSOR =  0x0400;
	
	public static final byte LANG_UNKNOWN  =  0;
	public static final byte LANG_PL       =  1;
	public static final byte LANG_EN       =  2;
	public static final byte LANG_CZ       =  3;
	public static final byte LANG_SK       =  4;
	public static final byte LANG_DE       =  5;
	public static final byte LANG_FR       =  6;
	public static final byte LANG_IT       =  7;
	public static final byte LANG_RU       =  8;
	
	public static final int ACTION_OPEN1   = 4;
	public static final int ACTION_OPEN2   = 5;
	
	public static final int SYS_STATE_OPENING1       =  0x01;
	public static final int SYS_STATE_OPENING2       =  0x02;
	public static final int SYS_STATE_OPENING3       =  0x04;
	public static final int SYS_STATE_SIPCONNECTED   =  0x08;
	public static final int SYS_STATE_GATEISCLOSED    = 0x10;
	public static final int SYS_STATE_GATEWAYISCLOSED = 0x20;
	public static final int SYS_STATE_PROXYREGISTERED = 0x40;
	
	public native int Init(char Language, String authkey, String serial, String clientid, String name, byte useproxy);
	public native void Release(int dc);
	public native int ProxyInit(int dc);
	public native int SetPingInterval(int dc, int interval);
	public native int Work(int dc);
	public native void AppendRecvBuffer(int dc, byte [] in, int in_len);
	public native byte[] GetSentBuffer(int dc, int size);
	public native void SetConnecting(int dc);
	public native String GetAuthkey(int dc);
	public native void SetDisconnected(int dc, int wait_for_reconnect);
	public native int RequestAction(int dc, int action, int param1, int param2, int param3, int param4);
	public native int OpenGate(int dc, int num);
	public native int SipConnect(int dc, int mute, int video);
	public native int SipDisconnect(int dc);
	public native void GetConnectionSettings(int dc, ConnectionSettings csettings, SipData Sip);
	public native int GetEvent(int dc, dEvent event); 
	public native int GetSysState(int dc, dSysState state);
	public native int GetLockEvent(int dc, dLockEvent event);
	public native int IsProxy(int dc);
	public native int IsAuthorized(int dc);
	public native void setVideoWindow(Object w);
	public native void setSpeakerOnOff(int dc, int on);
	public native int hasNeon();
	public native void RegisterPushID(int dc, String reg_id);
	
	public native int MemTest();
	
	public String SipServer(int dc) {
	    	
	    	ConnectionSettings csettings = new ConnectionSettings();
	    	
	    	GetConnectionSettings(dc, csettings, csettings.Sip);
	  
	    	return csettings.Sip.Host;
	}
	
    static {
        System.loadLibrary("domophone");
    }
}
