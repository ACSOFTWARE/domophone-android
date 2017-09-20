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
package com.acsoftware.android.domophone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.LangUtils;

import com.acsoftware.android.domophone.LibDomophone.ConnectionSettings;
import com.acsoftware.android.domophone.LibDomophone.SipData;

import android.app.Activity;
import android.content.IntentFilter.AuthorityEntry;
import android.graphics.drawable.shapes.ArcShape;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;


public class DomophoneConnection extends Thread {
	
	private static final String LOG_TAG = "DomophoneConnection";
	private int dc;
	private static final LibDomophone libd = new LibDomophone();
	private Handler mEventHandler;
	private Socket socket;
	private InputStream in_stream;
	private OutputStream out_stream;
	private String Host;
	private int Port;
	private Boolean _is_proxy;
	private String SerialKey;
	private Boolean mRun = Boolean.TRUE;
	private Boolean mRunning = Boolean.FALSE;
	private DomophoneConnection Proxy = null;
	private long LastReceiveDataTime = 0;

	private synchronized void SetRuning(Boolean r) {
        mRunning = r;
    }
	
	public synchronized Boolean IsRun() {
        return mRunning;
    }
	
	public synchronized Handler getEventHandler() {
		return mEventHandler;
	}
	
	public synchronized void setEventHandler(Handler mHandler) {
		mEventHandler = mHandler;
		
		if ( Proxy != null ) {
			Proxy.setEventHandler(mHandler);
		}
	}
	
	public synchronized void _cancel() {
		if ( !_is_proxy
				&& Proxy != null ) {
			Proxy._cancel();
		}
        mRun = false;
    }
	
	public synchronized boolean _canceled() {
		
        return mRun == false;
    }
	
	public Boolean isAuthorized() {
		return libd.IsAuthorized(dc) == 1 ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public LibDomophone.ConnectionSettings GetCS() {
	    LibDomophone.ConnectionSettings cs = libd.new ConnectionSettings();
	    libd.GetConnectionSettings(dc, cs, cs.Sip);
	    return cs;
	}
	

	
    public void openGate() { 
	    libd.OpenGate(dc, 2);
	}
    
    public void openGateway() {
		libd.OpenGate(dc, 1);
	}
    
    public void SipConnect(Boolean speaker_on, Boolean video) {
    	libd.SipConnect(dc, speaker_on ? 1 : 0, video ? 1 : 0);
    }
    

    public void SipDisconnect() {
        libd.SipDisconnect(dc);
    }
    
    public Boolean HasNeonFeatures() {
    	return libd.hasNeon() != 0;
    }
    
    public void SetSpeakerOn(Boolean on) {
    	libd.setSpeakerOnOff(dc, on ? 1 : 0);
    }
    
    public String SipServer() {
    	
    	return libd.SipServer(dc);
    }
    
	public DomophoneConnection(Handler _mHandler) {
		
		in_stream = null;
		out_stream = null;
		socket = null;
		setEventHandler(_mHandler);
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
    	   	dc = libd.Init((char)LibDomophone.LANG_PL, Authkey, _SerialKey, Clientid, android.os.Build.MANUFACTURER+ " "+android.os.Build.MODEL, LibDomophone.USEPROXY_ALWAYS);   			
    	   	
    	} else {
    		dc = _dc;
    	}
    	
    	SetRuning(true);
    	start();
 
    }

    private void sendMessage(int What) {
    	
    	sendMessage(What, null);
    }
    
    private void sendMessage(int What, Object obj) {
    	
    	Handler mHandler = getEventHandler();
    	
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
        	
    	if ( in_stream != null ) return Boolean.FALSE;
    	
    	if ( _is_proxy ) {
           GetProxyAddr();
    	}

        try {
             	
            InetAddress serverAddr = InetAddress.getByName(Host);
            
            Trace.d(LOG_TAG, "Connection To:"+serverAddr+" [PROXY="+_is_proxy+"]");
            
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverAddr, Port), _is_proxy ? 5000 : 2000);
                          	
        	in_stream = socket.getInputStream();
        	out_stream = socket.getOutputStream();

        	
            } catch (Exception e) {       
            	disconnect(); 
            }
      
        if ( in_stream == null) {
            try {            
                sleep(1000);
                } catch (Exception e) {}
        }
        
        return Boolean.TRUE;
    }
    
    private void disconnect() {
    
    	in_stream = null;
    	out_stream = null;
    
    	
    	try {
    		 if ( socket != null ) {
    	        	socket.close();
    	        	socket = null;  			 
    		 }

    	} catch (Exception e) { };
    	
    	libd.SetDisconnected(dc, 1);
    }
    
    public void RegisterPushID(String RegID) {
    	libd.RegisterPushID(dc, RegID);
    }
    

	public void run() {
		
		SetRuning(Boolean.TRUE);
		
		Trace.d(LOG_TAG, "run()");
		 
	    _is_proxy = libd.IsProxy(dc) == 0 ? Boolean.FALSE : Boolean.TRUE;
	   
	    
	    int wr;
	    
		try {
		
	    
			 while(!_canceled() && dc != 0) {
			        
		            
					if ( !_is_proxy && MainActivity.Connection != this ) {
						Trace.d(LOG_TAG, "DUBEL!!!!");
						_cancel();
						break;
					}
		            
					wr = libd.Work(dc);
					
		            switch(wr) {
		                case LibDomophone.WRESULT_ONCONNECT:
		                	Trace.d(LOG_TAG, "WRESULT_ONCONNECT "+dc+" [PROXY="+_is_proxy+"]");
		                	sendMessage(wr);
		                    break;
		                case LibDomophone.WRESULT_ONDISCONNECT:
		                	Trace.d(LOG_TAG, "WRESULT_ONDISCONNECT [PROXY="+_is_proxy+"]");
		                    disconnect();
		                    sendMessage(wr);
		                    break;
		                case LibDomophone.WRESULT_ONAUTHORIZE:
		            
		                	Trace.d(LOG_TAG, "WRESULT_ONAUTHORIZE [PROXY="+_is_proxy+"]");
		                   
		                    LibDomophone.ConnectionSettings cs = libd.new ConnectionSettings();
		                    libd.GetConnectionSettings(dc, cs, cs.Sip);
		                    
		                    sendMessage(wr, cs);
		                    
		                    break;
		                case LibDomophone.WRESULT_ONUNAUTHORIZE:
		                	Trace.d(LOG_TAG, "WRESULT_ONUNAUTHORIZE [PROXY="+_is_proxy+"]");
		                	sendMessage(wr);
		                    break;
		                case LibDomophone.WRESULT_ONEVENT:
		                	Trace.d(LOG_TAG, "WRESULT_ONEVENT [PROXY="+_is_proxy+"]");
		                	
		                	LibDomophone.dEvent event = libd.new dEvent();
		                	if ( libd.GetEvent(dc, event) == 1
		                		 && event.Duplicate == 0 ) {
		                		sendMessage(wr, event);
		                	}
		                    break;
		                case LibDomophone.WRESULT_TRYCONNECT:
		                	Trace.d(LOG_TAG, "WRESULT_TRYCONNECT [PROXY="+_is_proxy+"]");

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
				     
				                			LastReceiveDataTime = System.currentTimeMillis();
				                			libd.AppendRecvBuffer(dc, buff, readed);
				                		}
			                		} else {
			                			sleep(1);
			                		}
			                		
		
			                	} else {
			                		libd.SetDisconnected(dc, 0);
			                	}
		                	} catch (Exception e) { 
		                		Trace.d(LOG_TAG, "WRESULT_WAITFORDATA Exception: "+e.toString());
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
		                		Trace.d(LOG_TAG, "WRESULT_TRYSENDDATA Exception: "+e.toString());
		                		libd.SetDisconnected(dc, 0);
		                	} 
		                	
		                    break;
		                case LibDomophone.WRESULT_RESPONSETIMEOUT:
		                	Trace.d(LOG_TAG, "WRESULT_RESPONSETIMEOUT [PROXY="+_is_proxy+"]");
		                    break;
		                case LibDomophone.WRESULT_ONRESPONSE:
		                	Trace.d(LOG_TAG, "WRESULT_ONRESPONSE [PROXY="+_is_proxy+"]");
		                    break;
		                case LibDomophone.WRESULT_ONSYSSTATE:
		                
		                	LibDomophone.dSysState state = libd.new dSysState();
		                	if ( libd.GetSysState(dc, state) == 1 ) {
		                		Trace.d(LOG_TAG, "WRESULT_ONSYSSTATE [PROXY="+_is_proxy+"]");
		                		state._proxy_src = _is_proxy;
		                		sendMessage(wr, state);
		                	}
		                	
		                    break;
		                    
		                case LibDomophone.WRESULT_LOCKED:
		                	
		                	Trace.d(LOG_TAG, "WRESULT_LOCKED "+dc+" [PROXY="+_is_proxy+"]");
		                	
		                	LibDomophone.dLockEvent lockevent = libd.new dLockEvent();
		                	if ( libd.GetLockEvent(dc, lockevent) == 1 
		                			&& lockevent._Owner == 0 ) {
		                		sendMessage(wr, lockevent);
		                	}
		                	break;
		                	
		                case LibDomophone.WRESULT_PROXYCONNECT:
		
		                	Trace.d(LOG_TAG, "WRESULT_PROXYCONNECT "+dc+" [PROXY="+_is_proxy+"]");
		       
		                	if ( Proxy == null && !_is_proxy ) {
			                	Proxy = new DomophoneConnection(getEventHandler());		                	
			        			Proxy.startWithInitialize("", SerialKey, "", "", 0, libd.ProxyInit(dc));	
		                	}


		                    break;
		                case LibDomophone.WRESULT_PROXYDISCONNECT:
		                	Trace.d(LOG_TAG, "WRESULT_PROXYDISCONNECT [PROXY="+_is_proxy+"]");
		                	
		            	    if ( Proxy != null ) {
		            	        Proxy._cancel();
		            	        Proxy = null;
		            	    }
		            	    
		                    break;
		                case LibDomophone.WRESULT_VERSIONERROR:
		                    sendMessage(wr);
		                    break;
		                case LibDomophone.WRESULT_DEVICENOTFOUND:
		                    Trace.d(LOG_TAG, "WRESULT_DEVICENOTFOUND [PROXY="+_is_proxy+"]");
		                    break;
		                case LibDomophone.WRESULT_REGISTER_PUSH_ID:
		                	sendMessage(wr);
		                    break;
		                case LibDomophone.WRESULT_NONE:
							sleep(100);
		                    break;
		            }
	
		    }
			 

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   
		 Trace.d(LOG_TAG, "THREAD FINISHED [PROXY="+_is_proxy+"]");
		 SetRuning(false);
    }
	

	
}
