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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;



// Warning ! Do not rename the service !
public class GCMService extends GCMBaseIntentService {

	private String LOGTAG = "GCMService";
	public static final int GCM_ONREGISTER     = 2;
	public static final int GCM_ONUNREGISTER   = 3;
	public static final int GCM_REGISTERERROR  = 4;
	
	public GCMService() {
		
	}
	
    public void sendMessage(int What) {
    	sendMessage(What, null);
    }
    
    private void sendMessage(int What, Object obj) {
    	if ( MainActivity._gcm_handler != null ) {
    		MainActivity._gcm_handler.sendMessage(MainActivity._gcm_handler.obtainMessage(What, obj));
    	}
    }
	
	@Override
	protected void onError(Context context, String errorId) {
		sendMessage(GCM_REGISTERERROR, errorId);
		Trace.d(LOGTAG, "Error while registering push notification: " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	      		
		if ( intent.getExtras().getString("message").toString().equals("RING("+prefs.getString("pref_id", "").replace("-", "")+")") ) {
			UserNotification n = new UserNotification(this);
			n.showRingNotification();
		}
		
		Trace.d(LOGTAG, "Push notification received: ["+intent.getExtras().getString("message").toString()+"]");
		
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Trace.d(LOGTAG, "Registered push notification : " + regId);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("push_reg_id_key", regId);
		editor.commit();
		
		sendMessage(GCM_ONREGISTER);
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Trace.d(LOGTAG, "Unregistered push notification : " + regId);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("push_reg_id_key", null);
		editor.commit();
		
		sendMessage(GCM_ONUNREGISTER);
	}
	
	protected String[] getSenderIds(Context context) {
	    return new String[] { "878000791682" };
	}
}
