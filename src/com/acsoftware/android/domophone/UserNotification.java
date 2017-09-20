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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class UserNotification {
	
	private Context context;
	private static int nID = 0;
	private static long LastRingNotificationTime = 0;
	
	public UserNotification(Context context) {
		
		this.context = context;

	}

	private void shopwNotification_16(String Title, String Message) {

		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(context)
		        .setSmallIcon(R.drawable.appicon)
		        .setContentTitle(Title)
		        .setContentText(Message)
		        .setAutoCancel(true)
		        .setSound(MainActivity.getNotifySoundUri(context))
		        .setDefaults(
				    Notification.DEFAULT_LIGHTS
						    | Notification.DEFAULT_VIBRATE);
		
		Intent resultIntent = new Intent(context, MainActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

		stackBuilder.addParentStack(MainActivity.class);

		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(nID, mBuilder.build());
		nID++;
		Trace.d("UserNotifications", "(v. 16) nID:"+nID);
	}
	
	private void shopwNotification_5(String Title, String Message) {
		
		    Uri sound = MainActivity.getNotifySoundUri(context);
		
			Notification notif = new Notification();
			notif.icon = R.drawable.appicon;
			notif.iconLevel = 0;
			notif.when = System.currentTimeMillis();
			notif.flags &= Notification.FLAG_ONGOING_EVENT;
			notif.sound = sound;
			
			NotificationManager mNotificationManager =
				    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
			Intent resultIntent = new Intent(context, MainActivity.class);

			
			PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
			
			notif.setLatestEventInfo(context, Title, Message, resultPendingIntent);
			mNotificationManager.notify(nID, notif);
			nID++;
			Trace.d("UserNotifications", "(v. 5) nID:"+nID);

	}
	
	private void shopwNotification_11(String Title, String Message) {
		
		Uri sound = MainActivity.getNotifySoundUri(context);
		
		
		Notification notif = new Notification.Builder(context)
		.setContentTitle(Title)
		.setContentText(Message)
		.setSmallIcon(R.drawable.appicon)
		.setAutoCancel(true)
		.setDefaults(
				Notification.DEFAULT_LIGHTS
			      | Notification.DEFAULT_VIBRATE)
		.setSound(sound)				
		.setWhen(System.currentTimeMillis()).getNotification();
		

		NotificationManager mNotificationManager =
			    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Intent resultIntent = new Intent(context, MainActivity.class);

		
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
        notif.contentIntent = resultPendingIntent;
        mNotificationManager.notify(nID, notif);
		nID++;
		Trace.d("UserNotifications", "(v. 11) nID:"+nID);
		
	}
	
    public void showRingNotification() {
        
    	String Title = "DOMOPHONE";
    	String Message = "Dzwoni...";
    	
    	
    	if ( System.currentTimeMillis() - LastRingNotificationTime <= 1000 ) {
    		return;
    	}
    	
    	LastRingNotificationTime = System.currentTimeMillis();
    	
    	
        if (Version.sdkAboveOrEqual(16)) {
        	shopwNotification_16(Title, Message);
        } else if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
        	shopwNotification_11(Title, Message);
        } else {
        	shopwNotification_5(Title, Message);
        }

        //PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //WakeLock wakeLock = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        //wakeLock.acquire();
        
    }
}
