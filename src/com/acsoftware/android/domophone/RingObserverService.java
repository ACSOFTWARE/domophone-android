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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.acsoftware.android.domophone.LibDomophone.dEvent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class RingObserverService extends Service implements OnSharedPreferenceChangeListener {

	private static RingObserverService instance = null;
	private static String LOGTAG = "RingObserver-Service";
	private static DCT Connection = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	        Trace.d(LOGTAG, "Received start id " + startId + ": " + intent);
	        super.onStartCommand(intent, flags, startId);
	        return START_STICKY;
	}

	@Override
	public void onDestroy() {
		 instance = null;
	     Trace.d(LOGTAG, "onDestroy");
	     CloseConnection();
	}
	   
    @Override
    public void onCreate() {
    	instance = this;
    	connectionInit();
        Trace.d(LOGTAG, "OnCreate");
    }
    
    static public Boolean Exists() {
    	return instance != null;
    }
    
    private void CloseConnection() {
    	
	    if ( Connection != null ) {
	        Connection._cancel();
	        while(Connection.IsRun()) {
	        	SystemClock.sleep(100);
	        }
	        try {
				Connection.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	        Connection = null;
	    }
	    
    }
    
	private Handler _dc_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch(msg.what) {
				case LibDomophone.WRESULT_ONEVENT:
					Trace.d(LOGTAG, "LibDomophone.WRESULT_ONEVENT");
					
					dEvent event = (dEvent)msg.obj;
					if ( event.Scope == LibDomophone.ES_RING ) {
						
						UserNotification n = new UserNotification(RingObserverService.this);
						n.showRingNotification();	
						
					}

					break;
					
			}
			super.handleMessage(msg);
		}

	};
    
	private void connectionInit() {
		  	    
		
		CloseConnection();
	    
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int PORT = 0;
		String IP = prefs.getString("pref_ip", "");
		
		if ( IP.length() > 2 ) {
			
			IP = IP.replace(" ", "");
			IP = IP.replace(",", ".");
			int r = IP.indexOf(":");
			if ( r > 0 ) {
				try {
					PORT = Integer.parseInt(IP.substring(r+1));	
				}catch(Exception e) {PORT=0;};

				IP = IP.substring(0, r);
			}
			

			try {
		        Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
		        if ( pattern.matcher(IP).matches() == false ) {
		        	IP = "";
		        }
		    } catch (PatternSyntaxException ex) {
		        IP = "";
		    }
		}
		
		if ( PORT == 0 ) {PORT=465;};
		
		String ID = prefs.getString("pref_id", "");
		
		if ( ID.length() == 0 ) {
			 ID = "";
			 Random randomGenerator = new Random();
			 
			 for(int a=0;a<LibDomophone.ID_SIZE;a++) {
			
				    ID+=String.format("%02X", randomGenerator.nextInt(255));
			     
			        if ( (a+1)%2 == 0 && a<LibDomophone.ID_SIZE-1 ) {
			            ID+= "-";
			        }
			    };
			 
			 Editor editor = prefs.edit();
			 editor.putString("pref_id", ID);
			 editor.commit();
		}
		

	    if ( IP.length() > 2
	    	 && ID.length() != 0 ) {
	    	
	    	ID = "FFFF"+ID.substring(4);
	    			
			Connection = new DCT(_dc_handler);
			Connection.startWithInitialize(
					prefs.getString("pref_authkey", ""),
					prefs.getString("pref_serialkey", ""),
					ID, IP, PORT, 0);
	    }
	    
	    
	    
	}
	
    
    private class DCT extends Thread {
    	
    	private static final String LOG_TAG = "DCT";
    	private int dc;
    	private LibDomophone libd;
    	private Handler mHandler;
    	private Socket socket;
    	private InputStream in_stream;
    	private OutputStream out_stream;
    	private String Host;
    	private int Port;
    	private Boolean _is_proxy;
    	private String SerialKey;
     	private Boolean mRun = Boolean.TRUE;
	    private Boolean mRunning = Boolean.FALSE;
	    private DCT Proxy = null;
    	
		private synchronized void SetRuning(Boolean r) {
	        mRunning = r;
	    }
		
		public synchronized Boolean IsRun() {
	        return mRunning;
	    }
		
		public synchronized void _cancel() {
			if ( !_is_proxy
					&& Proxy != null ) {
				Proxy._cancel();
			}
	        mRun = false;
	    }
		
		public synchronized Boolean _canceled() {
			return !mRun;
		}
    	
       
    	public DCT(Handler _mHandler) {
    		
    		in_stream = null;
    		out_stream = null;
    		socket = null;
    		mHandler = _mHandler;
    		libd = new LibDomophone();
    		dc = 0;

    	}
    	
    	protected void finalize () {    

    		if ( dc != 0 ) {
    	        libd.Release(dc);
    	        dc = 0;
    	        disconnect();
    		}

            Trace.d(LOG_TAG, "Finalize");
        }

        public void startWithInitialize(String Authkey,  String _SerialKey,  String Clientid, String RemoteHost, int RemotePort, int _dc) {

        	Host = RemoteHost;
        	Port = RemotePort;
        	SerialKey = _SerialKey;
        	
        	if ( _dc == 0 ) {
        		dc = libd.Init((char)LibDomophone.LANG_PL, Authkey, _SerialKey, Clientid, "AndroidOS", LibDomophone.USEPROXY_ALWAYS);
        		
        	} else {
        		dc = _dc;
        	}
        	
        	libd.SetPingInterval(dc, libd.IsProxy(dc) == 0 ? 120 : 50);
        	
        	SetRuning(true);
        	start();
        	
        }
        
        private void sendMessage(int What, Object obj) {
        	if ( mHandler != null ) {
            	mHandler.sendMessage(mHandler.obtainMessage(What, obj));
        	}
        }
        
        private void GetProxyAddr() {
        	
        	Host = "";
        	Port = 465;
        	
        	Trace.d(LOG_TAG, "GetProxy");

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://www.acsoftware.pl/support/domophone.php");

            try {
                // Add your data
                List <NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("Action", "GetProxyAddress"));
                nameValuePairs.add(new BasicNameValuePair("SerialKey", SerialKey));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                
                String[] responseBody = EntityUtils.toString(httpclient.execute(httppost).getEntity()).split(":");

                if ( responseBody.length > 0 ) {
                	
                 	Trace.d(LOG_TAG, "responseBody:"+responseBody);
                	 Host = responseBody[0];
                	 if ( responseBody.length > 1 ) {
                		 try {
                			 Port = Integer.parseInt(responseBody[1]);
                		 } catch (Exception e) {
                		 }
                	 }
                	
                }
               
                
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
        }
        
    	
        private Boolean TryConnect() {

        	disconnect();
        	
        	if ( _is_proxy ) {
                GetProxyAddr();
         	}
        	
        	if ( in_stream != null ) return Boolean.FALSE;
        	
            try {
                
                InetAddress serverAddr = InetAddress.getByName(Host);
                
                Trace.d(LOG_TAG, "Connection To:"+serverAddr+" [PROXY="+_is_proxy+"]");
                
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, Port), _is_proxy ? 10000 : 5000);
                                     	
            	in_stream = socket.getInputStream();
            	out_stream = socket.getOutputStream();
            	
                } catch (Exception e) {              
                	disconnect(); 
                }
            

            if ( in_stream == null) {
                try {            
                    sleep(30000);
                    } catch (Exception e) {}
            }
              
            return Boolean.TRUE;
        }
        
        private void disconnect() {
        
        	in_stream = null;
        	out_stream = null;
        	
        	try {
        		 if ( socket != null ) {
        			 Trace.d(LOG_TAG, "Socket Close");
        	        	socket.close();
        	        	socket = null;  			 
        		 }

        	} catch (Exception e) { };
        	
        	libd.SetDisconnected(dc, 1);
        }
                
    	public void run() {
    		 
    		SetRuning(Boolean.TRUE);
    	
    	    _is_proxy = libd.IsProxy(dc) == 0 ? Boolean.FALSE : Boolean.TRUE;
    	    
    	    
    	    int wr;
    	    
    		try {
    	    
    			 while(!_canceled() && dc > 0) {
    			     
    				    if ( !_is_proxy 
    				    		&& RingObserverService.Connection != this ) {
    				    	_cancel();
    				    	break;
    				    }
    				 
    		            wr = libd.Work(dc);
    		            
    		            switch(wr) {
    		                case LibDomophone.WRESULT_ONDISCONNECT:
    		                	Trace.d(LOG_TAG, "RO_WRESULT_ONDISCONNECT [PROXY="+_is_proxy+"]");
    		                    disconnect();
    		                    break;
    		                case LibDomophone.WRESULT_ONUNAUTHORIZE:
    		                	Trace.d(LOG_TAG, "RO_WRESULT_ONUNAUTHORIZE [PROXY="+_is_proxy+"]");
    		                    break;
    		                case LibDomophone.WRESULT_ONEVENT:
    		                	Trace.d(LOG_TAG, "RO_WRESULT_ONEVENT [PROXY="+_is_proxy+"]");
    		                	
    		                	LibDomophone.dEvent event = libd.new dEvent();
    		                	if ( libd.GetEvent(dc, event) == 1
    		                		 && event.Duplicate == 0 ) {
    		                		sendMessage(wr, event);
    		                	}
    		                    break;
    		                case LibDomophone.WRESULT_TRYCONNECT:
    		                	Trace.d(LOG_TAG, "RO_WRESULT_TRYCONNECT [PROXY="+_is_proxy+"]");

    		                	if ( TryConnect() ) {
    		                		libd.SetConnecting(dc);       		
    		                	}
    		            	              
    		                    break;
    		                case LibDomophone.WRESULT_WAITFORDATA:
    		                	try {
    			                	if ( in_stream != null ) {
    			                		byte[] buff = new byte[1024];
    			                		
    			                		if ( in_stream.available() > 0 ) {
    				                		int readed = in_stream.read(buff);
    				                		if ( readed > 0 ) {
    				                			/*
				                				String S = "";
				                				for(int a=0;a<readed;a++) {
				                					S+=String.valueOf((int)(buff[a] & 0xff))+",";
				                				}
				                				Trace.d(LOG_TAG, "Data readed ("+readed+"): "+S+" [PROXY="+_is_proxy+"]");
    				                			*/
    				                			libd.AppendRecvBuffer(dc, buff, readed);
    				                		}
    			                		} else {
    			                			sleep(1);
    			                		}
    		
    			                	} else {
    			                		libd.SetDisconnected(dc, 0);
    			                	}
    		                	} catch (Exception e) { 
    		                		Trace.d(LOG_TAG, "RO_WRESULT_WAITFORDATA Exception: "+e.toString());
    		                		libd.SetDisconnected(dc, 0);
    		                	} 
    		                    break;
    		                    
    		                case LibDomophone.WRESULT_TRYSENDDATA:
    		                	
    		                	try {
    			                	if ( out_stream != null ) {
    			                		
    			                		byte[] buff = libd.GetSentBuffer(dc, 1024);
    			                		
    		                			if ( buff.length > 0 ) {
    		                				/*
    		                				String S = "";
    		                				for(int a=0;a<buff.length;a++) {
    		                					S+=String.valueOf((int)(buff[a] & 0xff))+",";
    		                				}
    		                				Trace.d(LOG_TAG, "Data to send ("+buff.length+"): "+S+" [PROXY="+_is_proxy+"]");
    		                				*/
        			                		out_stream.write(buff);
        			                		buff = null;
    		                			}      
    			                	
    			                	} else {
    			                		libd.SetDisconnected(dc, 0);
    			                	}
    		                	} catch (Exception e) { 
    		                		Trace.d(LOG_TAG, "RO_WRESULT_TRYSENDDATA Exception: "+e.toString());
    		                		libd.SetDisconnected(dc, 0);
    		                	} 
    		                	
    		                    break;
    		                	
    		                case LibDomophone.WRESULT_PROXYCONNECT:
    		
    		                	Trace.d(LOG_TAG, "RO_WRESULT_PROXYCONNECT [PROXY="+_is_proxy+"]");
    		       
    		                	if ( Proxy == null ) {
    			                	Proxy = new DCT(mHandler);		                	
    			        			Proxy.startWithInitialize("", SerialKey, "", "", 0, libd.ProxyInit(dc));	
    		                	}


    		                    break;
    		                case LibDomophone.WRESULT_PROXYDISCONNECT:
    		                	Trace.d(LOG_TAG, "RO_WRESULT_PROXYDISCONNECT [PROXY="+_is_proxy+"]");
    		                	
    		            	    if ( Proxy != null ) {
    		            	        Proxy._cancel();
    		            	        Proxy = null;
    		            	    }
    		            	    
    		                    break;
    		                    
    		                case LibDomophone.WRESULT_ONSYSSTATE:
    		                	Trace.d(LOG_TAG, "RO_WRESULT_ONSYSSTATE [PROXY="+_is_proxy+"]");
    		                    break;
    		                    
    		                case LibDomophone.WRESULT_NONE:
    							sleep(100);
    		                    break;
    		            }
    	
    		    }
    			 
    			 Trace.d(LOG_TAG, "RO_THREAD BEFORE FINISH [PROXY="+_is_proxy+", mRun="+mRun+", dc="+dc+"]");
    			disconnect();
    			 
         	    if ( Proxy != null ) {
        	        Proxy._cancel();
        	        while(Proxy.IsRun()) {
        	        	sleep(200);
        	        }
        	        Proxy.join();
        	        Proxy = null;
        	    }
         	    
         	    libd.Release(dc);
         	    dc = 0;
    	
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    	   
    		 Trace.d(LOG_TAG, "RO_THREAD FINISHED [PROXY="+_is_proxy+"]");
    		 SetRuning(false);
        }
    	

    	
    }


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		if ( key.equals("pref_ip")
				|| key.equals("pref_authkey")) {
				connectionInit();
		}
		
	}
	
	static public Boolean PrefOn(Context context) {
		Boolean Result = true;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Result = prefs.getBoolean("bg_mode", true);
		
		return Result;
	}
}
