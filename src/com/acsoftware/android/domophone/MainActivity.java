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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.acsoftware.android.domophone.LibDomophone.ConnectionSettings;
import com.acsoftware.android.domophone.LibDomophone.dEvent;
import com.acsoftware.android.domophone.LibDomophone.dLockEvent;
import com.google.android.gcm.GCMRegistrar;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.MediaRecorder.VideoSource;
import android.media.audiofx.AcousticEchoCanceler;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.drm.DrmStore.RightsStatus;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import static android.content.Intent.ACTION_MAIN;
import static android.media.AudioManager.STREAM_VOICE_CALL;
import android.media.audiofx.AcousticEchoCanceler;

@SuppressLint({ "NewApi", "NewApi" })
public class MainActivity extends Activity implements OnClickListener, OnSharedPreferenceChangeListener {

	String LOGTAG = "MainActivity";
	
	Handler _dc_handler;
	Handler _lp_handler;

	
	private boolean fv_warning = false;
	private long proxy_warning = 0;
	
	private boolean LargeScreen = false;
	public static DomophoneConnection Connection = null;
	
	private static LibLP LLP = null;
	private static final Object _lock= new Object(); 
	
	public static Handler _gcm_handler;
	private AudioManager mAudioManager;
    
	private static final int ACDEVCAP_AUDIO     = 0x1;
	private static final int ACDEVCAP_VIDEO     = 0x2;
	private static final int ACDEVCAP_GATEWAY   = 0x4;
	private static final int ACDEVCAP_GATE      = 0x8;
	
	private static final int STATUS_CONNECTING      = 0;
	private static final int STATUS_AUTHERROR       = 1;
	private static final int STATUS_CONNECTIONERROR = 2;
	private static final int STATUS_COMPATERROR     = 3;
	private static final int STATUS_CONNECTED       = 4;
	private static final int STATUS_OPENING         = 5;
	private static final int STATUS_WAITING         = 6;
	
	private static final int IMG_GATE_BIG_G         = 1;
	private static final int IMG_GATE_BIG_HL        = 2;
	private static final int IMG_GATEWAY_BIG_G      = 3;
	private static final int IMG_GATEWAY_BIG_HL     = 4;
	private static final int IMG_GATE_BIG_W         = 5;
	private static final int IMG_GATE_G             = 6;
	private static final int IMG_GATEWAY_G          = 7;
	
	
	private static Boolean GateIsOpen         = true;
	private static Boolean GatewayIsOpen      = true;
	
	Timer updateTimer1;
	Timer hideButtonTimer1;
	Timer timeoutTimer1;
	Timer hideLogTimer1;
	Timer videoTimer1;
	Timer sipTimeoutTimer1;
	Timer sipErrorTimer1;
	
	Button infoBtn1;
	ImageView logoImage;
	ImageView statusImage;
	TextView statusLabel;
	TextView productHomePage;
	TextView mfrHomePage;
	RelativeLayout rootLayout;
	RelativeLayout btnsLayout;
	RelativeLayout logLayout;
	RelativeLayout videoLayout;
	ProgressBar actInd;
	TextView logView;
	
	Button gateBtn;
	Button gatewayBtn;
	Button videoBtn;
	Button voiceBtn;
	Button openBtn;
	Button openBtnRef;
	Button settingsBtn;
	
	int currentStatusID;

	int Caps;
	long LastSysStateTime = 0;
	long LastAudioVideoTouch = 0;
	
	
	SurfaceView mVideoView = null;
	AndroidVideoWindowImpl androidVideoWindowImpl = null;
	
	boolean updateInProgress;
	boolean inForeground;
	
    AsyncTask<Void, Void, Void> mRegisterTask;
    
    int nID = 0;
    
	private Timer mTimer = null;
	
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	inForeground = false;
    	
    	Trace.d(LOGTAG, "onPause");
    	sipTerminate();
    	
    	reinitIterateTimer(250);
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	
    	inForeground = true;
    }
    

       
    @Override
    public void onCreate(Bundle savedInstanceState) {
    
   
        super.onCreate(savedInstanceState);
        
        LargeScreen = getResources().getBoolean(R.bool.sw600dp);
        
        if ( LargeScreen ) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
        
        mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
        initPushNotificationService(getApplicationContext()); 
        
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_main);
         
        updateTimer1 = null;
        hideButtonTimer1 = null;
        timeoutTimer1 = null;
        hideLogTimer1 = null;
        videoTimer1 = null;
        sipTimeoutTimer1 = null;
        sipErrorTimer1 = null;
        
        Caps = 0;
        currentStatusID = 0;
        updateInProgress = false;
        inForeground = true;

        logoImage = (ImageView) findViewById(R.id.imageView3);  
        
        statusImage = (ImageView) findViewById(R.id.imageView4);
        statusImage.setVisibility(View.INVISIBLE);
        statusLabel = (TextView) findViewById(R.id.textView1);
        statusLabel.setText("");
        actInd = (ProgressBar) findViewById(R.id.progressBar1);
        actInd.setVisibility(View.INVISIBLE);
        
        infoBtn1 = (Button) findViewById(R.id.infoBtn1);
        infoBtn1.setOnClickListener(this);
        rootLayout = (RelativeLayout) findViewById(R.id.main_activity);
      
      
		final View BtnsView = getLayoutInflater().inflate(R.layout.btns_layout, rootLayout, false);
	    rootLayout.addView(BtnsView);
	    btnsLayout = (RelativeLayout) findViewById(R.id.btns_layout);
	    btnsLayout.setVisibility(View.INVISIBLE);
	    
		final View LogView = getLayoutInflater().inflate(R.layout.log_layout, rootLayout, false);
	    rootLayout.addView(LogView);
	    logLayout = (RelativeLayout) findViewById(R.id.log_layout);
	    logLayout.setVisibility(View.VISIBLE);
	    
	    logView = (TextView) findViewById(R.id.logView);
		
        gateBtn = (Button) findViewById(R.id.button1);
        gateBtn.setOnClickListener(this);
        gatewayBtn = (Button) findViewById(R.id.button2);
        gatewayBtn.setOnClickListener(this);
        voiceBtn = (Button) findViewById(R.id.button3);
        voiceBtn.setOnClickListener(this);
        videoBtn = (Button) findViewById(R.id.button4);
        videoBtn.setOnClickListener(this);

        openBtn = (Button) findViewById(R.id.openBtn1);
        openBtn.setOnClickListener(this);
        
        settingsBtn = (Button) findViewById(R.id.settingsBtn1);
        settingsBtn.setOnClickListener(this);
       
		final View VideoView = getLayoutInflater().inflate(R.layout.video_layout, rootLayout, false);
		if ( VideoView != null ) {
			 rootLayout.addView(VideoView);
		}
		videoLayout = (RelativeLayout) findViewById(R.id.video_layout);
		videoLayout.setVisibility(View.INVISIBLE);
        mVideoView = (SurfaceView) findViewById(R.id.videoSurface);
        

        androidVideoWindowImpl = new AndroidVideoWindowImpl(mVideoView, null);
        
        androidVideoWindowImpl.setListener(new AndroidVideoWindowImpl.VideoWindowListener() {
                public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
                      //  LinphoneManager.getLc().setVideoWindow(vw)
                	    Trace.d("androidVideoWindowImpl", "onVideoRenderingSurfaceReady");
                        mVideoView = surface;
                }

                public void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw) {
                	Trace.d("androidVideoWindowImpl", "onVideoRenderingSurfaceDestroyed");
                    //    LinphoneCore lc = LinphoneManager.getLc();
                    //    if (lc != null) {
                    //            lc.setVideoWindow(null);
                    //    }
                }

	
				public void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
					Trace.d("androidVideoWindowImpl", "onVideoRenderingSurfaceDestroyed");
					//mCaptureView = surface;
					//LinphoneManager.getLc().setPreviewWindow(onVideoPreviewSurfaceReady);
				}

				@Override
				public void onVideoPreviewSurfaceDestroyed(
						AndroidVideoWindowImpl vw) {
					
				}

        });
        
        androidVideoWindowImpl.init();
                
    	_dc_handler = new Handler() {
    		@Override
    		public void handleMessage(Message msg) {

    			switch(msg.what) {
    				case LibDomophone.WRESULT_ONDISCONNECT:
    					cevent_Disconnected();
    					break;
    				case LibDomophone.WRESULT_ONUNAUTHORIZE:
    					cevent_Unauthorized();
    					break;
    				case LibDomophone.WRESULT_ONAUTHORIZE:
    					cevent_Authorized((ConnectionSettings) msg.obj);
    					break;
    				case LibDomophone.WRESULT_ONEVENT:
    					cevent_Event((dEvent) msg.obj);
    					break;
    				case LibDomophone.WRESULT_VERSIONERROR:
    					cevent_VersionError();
    					break;
    				case LibDomophone.WRESULT_LOCKED:
    					cevent_Locked((dLockEvent) msg.obj);
    					break;
    				case LibDomophone.WRESULT_ONSYSSTATE:
    					cevent_SysState((LibDomophone.dSysState) msg.obj);
    					break;
    				case LibDomophone.WRESULT_REGISTER_PUSH_ID:
    					cevent_RegisterPushID();
    					break;
    					
    			}
    			super.handleMessage(msg);
    		}

    	};
    	
    	
    	_lp_handler = new Handler() {
    		@Override
    		public void handleMessage(Message msg) {

    			switch(msg.what) {
    				case LibLP.LPEVENT_BEFORE_REGISTER:
    					Trace.d(LOGTAG, "LibLP.LPEVENT_BEFORE_REGISTER");
    					setConnectedStatusWithActInd(true);
    					break;
    				case LibLP.LPEVENT_REGISTERED:
    					Trace.d(LOGTAG, "LibLP.LPEVENT_REGISTERED");
    					SipRegistered();
    					break;
    				case LibLP.LPEVENT_CALLSTARTED:
    					Trace.d(LOGTAG, "LibLP.LPEVENT_CALLSTARTED");
    					LLP.EnableEchoCancellation(true);
    					sipCallStarted();
    					break;
    				case LibLP.LPEVENT_DO_TERMINATE:
    					Trace.d(LOGTAG, "LibLP.LPEVENT_DO_TERMINAT");
    					sipTerminate();
    					break;
    				case LibLP.LPEVENT_SIP_REGISTRATION_FAILED:
    					Trace.d(LOGTAG, "LibLP.SIP_REGISTRATION_FAILED");
    					SipError();
    					break;
    				case LibLP.LPEVENT_SHOW_VIDEO_WINDOW:
    					Trace.d(LOGTAG, "LibLP.LPEVENT_SHOW_VIDEO_WINDOW");
    					sipVideoStarted();
    					break;
    					
    			}
    			super.handleMessage(msg);
    		}

    	};

    	
        rootLayout.post(new Runnable() { 
            public void run() { 
         
            	Trace.i("Density:", Float.toString(getResources().getDisplayMetrics().density));
            	
            	
            	/*
            	 0.75 - ldpi
                 1.0 - mdpi
                 1.5 - hdpi
                 2.0 - xhdpi
                 3.0 - xxdpi
            	 */
                	
                setMargins(logoImage, 0, getLogoTop(), 0, 0);
                setMargins(logView, (int)(10 * getResources().getDisplayMetrics().density), rootLayout.getHeight() - getStatusBarHeight() - (int)((LargeScreen ? 150 : 60) * getResources().getDisplayMetrics().density), getBottomMargin() + voiceBtn.getWidth(), getStatusBarHeight() + (int)(5 * getResources().getDisplayMetrics().density));
               
            	
                setOpenBtnMargins();
                
                              
                updateLogoAndButtonsPosition();                       	
                setNotConnectedStatus();
                
                if ( Connection == null 
                		|| !Connection.isAuthorized() ) {
                	connectionInit();
                } else {
                	Connection.setEventHandler(_dc_handler);
                	cevent_Authorized(Connection.GetCS());
                }
              
            } 
           }); 
        
      
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        
    }
    
    void StartStopROService() {
    	
		if ( RingObserverService.PrefOn(this) ) {
			
			Trace.d(LOGTAG, "Start RingObserverService");
			
			if (!RingObserverService.Exists())  {
				startService(new Intent(ACTION_MAIN).setClass(this, RingObserverService.class));
				Trace.d(LOGTAG, "!RingObserverService.Exists()");
			} else {
				Trace.d(LOGTAG, "RingObserverService.Exists()");
			}
			
		} else if ( RingObserverService.Exists() ) {

			Trace.d(LOGTAG, "Stop RingObserverService");
			
			stopService(new Intent(ACTION_MAIN).setClass(this, RingObserverService.class));
		}
    }
    
    private void reinitIterateTimer(int interval) {
    	
        TimerTask lpTask = new TimerTask() {
            @Override
            public void run() {
            	if ( LLP != null ) {
            		LLP.Iterate();
            	}
                   
            }
        };
        
		if ( mTimer != null ) {
			mTimer.cancel();
			mTimer = null;
		}

		mTimer = new Timer("LP scheduler");
		mTimer.schedule(lpTask, 0, interval);
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		
		StartStopROService();
		
		reinitIterateTimer(20);

        if ( LastSysStateTime > 0 
        	 && System.currentTimeMillis() - LastSysStateTime > 10000 ) {
			connectionInit();
        }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	
		
        if ( LLP != null ) {
        	LLP.Clean();
        	LLP = null;
        }
        
        if ( Connection != null ) {
        	Connection.setEventHandler(null);
        }
		
        try {
    	         if ( mTimer != null ) {
    	    	    mTimer.cancel(); 	
    	    	    mTimer = null;
    	         }     
                 
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
	}
    
    
    private void setOpenBtnMargins() {
    	
    	Drawable d = getResources().getDrawable(getImg(IMG_GATE_BIG_W));
    	int h = d.getIntrinsicHeight();
    	int w = d.getIntrinsicWidth();
    	
    	int x = videoLayout.getVisibility() == View.VISIBLE ? mVideoView.getTop() + mVideoView.getHeight() : getLogoTop() + logoImage.getHeight();
    	
    
      	setMargins(openBtn, 
      		     	( rootLayout.getWidth() - ( btnsVert() ? voiceBtn.getWidth() + getRightMargin() : 0 ) ) / 2 - w / 2, 
      		     	0, 
      			   0, 
      			   ( rootLayout.getHeight() - x - getStatusBarHeight()) / 2 + getStatusBarHeight() - h/2 );
    }
    
    private void setMargins(View v, int left, int top, int right, int bottom) { 
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)v.getLayoutParams();
        lp.setMargins(left, top, right, bottom);

        v.setLayoutParams(lp);
    }
    
    private int getStatusBarHeight() {
    	return (int)((LargeScreen ? 70 : 45) * getResources().getDisplayMetrics().density);
    }
    
    private int getLogoTop() {
    	return (int)(rootLayout.getHeight()-getStatusBarHeight()) / 4;
    }
    
    private int getRightMargin() {
    	return (int)((LargeScreen ? 30 : 15) * getResources().getDisplayMetrics().density);
    }
    
    private int getBottomMargin() {
    	return getStatusBarHeight() + (int)((LargeScreen ? 50 : 15) * getResources().getDisplayMetrics().density);
    	
    }
    
    private int getBtnsYPos() {
    	if ( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
    		return (int)(50 * getResources().getDisplayMetrics().density);
    	} else {
    		if ( btnsVert() ) {
    			return getLogoTop() + logoImage.getHeight() + (int)((LargeScreen ? 50 : 20) * getResources().getDisplayMetrics().density);
    		} else {
    			return (int)(5 * getResources().getDisplayMetrics().density);
    		}
    		
    	}
    	
    }
    
    private boolean btnsVert() {
    	return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || videoLayout.getVisibility() == View.INVISIBLE;
    }
    
    private int getBtnsYPos(Boolean Video) {
    	
    	if ( Video ) {
    		return (int)(5 * getResources().getDisplayMetrics().density);
    	} else {
    		return getRightMargin();
    	}
    	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
    	
    	if ( item.getItemId() == R.id.menu_settings ) {
    		Intent i = new Intent("android.intent.action.PREFERENCES");
    		startActivity(i);
    		return true;
    	} else {
    		return super.onOptionsItemSelected(item);
    	}
    	
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
            if ( event.getAction() == MotionEvent.ACTION_DOWN ) {
            	RelativeLayout infoLayout = (RelativeLayout) findViewById(R.id.info_layout);
            	if ( infoLayout != null ) {
            		onClick(infoBtn1);
            		return true;
            	}
            }
        }
 
        return super.onTouchEvent(event);
    }
    
    private void setBaseStatus(String txt, int img, int StatusID) {
    	
    	statusImage.setVisibility(View.VISIBLE);
    	statusImage.setImageResource(img);
    	actInd.setVisibility(View.INVISIBLE);
    	currentStatusID = StatusID;
    	statusLabel.setText(txt);
    }
    
    private void setActStatus(String txt, int StatusID) {
    	
      	statusImage.setVisibility(View.INVISIBLE);
    	actInd.setVisibility(View.VISIBLE);
    	currentStatusID = StatusID;
    	statusLabel.setText(txt);

    }
    
    
    private void setConnectingStatus() {
    	setActStatus(getResources().getString(R.string.status_connecting), STATUS_CONNECTING);
    }

    private void setUnauthorizedStatus() {
    	setBaseStatus(getResources().getString(R.string.status_invalid_key), R.drawable.key, STATUS_AUTHERROR);
    }

    private void setNotConnectedStatus() {
    	setBaseStatus(getResources().getString(R.string.status_noconnection), R.drawable.error, STATUS_CONNECTIONERROR);
    }

    private void setVersionErrorStatus() {
    	setBaseStatus(getResources().getString(R.string.status_compat_error), R.drawable.error, STATUS_COMPATERROR);
    }

    private void setConnectedStatusWithActInd(boolean ai) {
        if ( ai ) {
        	setActStatus(getResources().getString(R.string.status_connected), STATUS_CONNECTED);
        } else {
        	setBaseStatus(getResources().getString(R.string.status_connected), R.drawable.ok, STATUS_CONNECTED); 
        }
    }

    private void setOpeningStatus() {
    	setActStatus(getResources().getString(R.string.status_opening), STATUS_OPENING);
    }

//    private void setWaitingStatus() {
//    	setActStatus(getResources().getString(R.string.status_waiting), STATUS_WAITING);
//    }

    public void addLogItem(String Log, String SenderName, Boolean Show) {
    	
    	if ( Show ) {
    		if ( hideLogTimer1 != null ) {
    			hideLogTimer1.cancel();
    			hideLogTimer1 = null;
    		}
    		
    		hideLogTimer1 = new Timer();
    		hideLogTimer1.schedule(new hideLogTimer1Task(), 15000);
    		
    		if ( logView.getVisibility() == View.INVISIBLE ) {
    			logView.setText("");
    			logView.setVisibility(View.VISIBLE);
    		}
    	}
    	
    	String L = logView.getText().toString();
    	if ( L.length() > 0 ) {
    		L+="\n";
    	}
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    
    	Log = sdf.format(new Date()) + " " + Log;
    	
    	if ( SenderName != null
    	     && SenderName.length() > 0 ) {
    		Log = Log + " - " + SenderName;
    	}
    	
    	logView.setText(L+Log);
    }
     
    private void showActionButtons(boolean Show, final AnimationListener listener) {
    
    	
    	int bcount = 0;
    	
    	if ( (Caps & ACDEVCAP_AUDIO) > 0 ) bcount++;    	
    	if ( (Caps & ACDEVCAP_VIDEO) > 0 ) bcount++;   	
    	if ( (Caps & ACDEVCAP_GATEWAY) > 0 ) bcount++;    	
    	if ( (Caps & ACDEVCAP_GATE) > 0 ) bcount++;
    	
        if ( !Connected() || bcount <=0 ) {
            Show = false;
        }
        
    	if ( ( btnsLayout.getVisibility() == View.VISIBLE ) == Show ) {
      		
  			if ( listener != null ) {
  				listener.onAnimationEnd(null);		
  			}
  			
      		return;
      	}
    	
    	Trace.d("Main", "showActionButtons:"+( btnsLayout.getVisibility() == View.VISIBLE )+", Show:"+Show);
    	
           	
    	Boolean vert = btnsVert();
    	int spaceing = 0;
    	int X = voiceBtn.getLeft();
    	int Y = voiceBtn.getTop();
    	int btnsYpos = getBtnsYPos();

    	if ( bcount <= 0 ) {
    		bcount = 1;
    	}
    	
        int div = (bcount-1);
        if (div == 0 ) div = 1;
        
        if ( vert ) {
            
            X = rootLayout.getWidth() - voiceBtn.getWidth() - getRightMargin();
            Y =  rootLayout.getHeight() - voiceBtn.getHeight() - getBottomMargin();
            
            spaceing = ((rootLayout.getHeight() - btnsYpos - getBottomMargin() ) - ( bcount * voiceBtn.getHeight() )) / div + voiceBtn.getHeight();
                        
        } else {
        	
            X = rootLayout.getWidth() - getRightMargin() - voiceBtn.getWidth();
            Y =  btnsYpos;

            spaceing = (( rootLayout.getWidth() - getRightMargin() * 2 ) - ( bcount * voiceBtn.getWidth())) / div + voiceBtn.getWidth();
        };

       
		btnsLayout.setVisibility(View.VISIBLE);


        if (  (Caps & ACDEVCAP_VIDEO) > 0 ) {
            
            videoBtn.setVisibility(View.VISIBLE);            
    	    setMargins(videoBtn, X, Y, 0, 0);
    	    
            if ( vert ) {
                Y -= spaceing;
            } else {
                X -= spaceing;
            }
            
    	} else {
    		videoBtn.setVisibility(View.INVISIBLE);
    	}

        voiceBtn.setVisibility(View.VISIBLE);  
        setMargins(voiceBtn, X, Y, 0, 0);
        
        if ( vert ) {
            Y -= spaceing;
        } else {
            X -= spaceing;
        }
        
        if (  (Caps & ACDEVCAP_GATEWAY) > 0 ) {
            
            gatewayBtn.setVisibility(View.VISIBLE);            
    	    setMargins(gatewayBtn, X, Y, 0, 0);
    	    
            if ( vert ) {
                Y -= spaceing;
            } else {
                X -= spaceing;
            }
            
    	} else {
    		gatewayBtn.setVisibility(View.INVISIBLE);
    	}
        
        if (  (Caps & ACDEVCAP_GATE) > 0 ) {
            
            gateBtn.setVisibility(View.VISIBLE);            
    	    setMargins(gateBtn, X, Y, 0, 0);
    	} else {
    		gateBtn.setVisibility(View.INVISIBLE);
    	}

    	
    	int startX, endX, startY, endY;
    	
    	if ( vert ) {
            startX = Show ? rootLayout.getWidth() - videoBtn.getLeft() : 0;
            endX = Show ? 0 : rootLayout.getWidth() - videoBtn.getLeft();
            startY = 0;
            endY = 0;
    	} else {
            startY = Show ? (Y+videoBtn.getWidth())*-1 : 0;
            endY = Show ? 0 : (Y+videoBtn.getWidth())*-1;	
            startX = 0;
            endX = 0;
    	}
		
    	
		TranslateAnimation ta = new TranslateAnimation(startX, endX, startY, endY);
		ta.setDuration(500);
		ta.setFillAfter(true);
		
		final boolean _Show = Show;
		
		ta.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				btnsLayout.setVisibility(_Show ? View.VISIBLE : View.INVISIBLE);
				if ( listener != null ) {
					listener.onAnimationEnd(null);		
				}
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}


		});
		
		btnsLayout.startAnimation(ta);
		
    }
    
    
    private void showLogoImage(final boolean Show, final AnimationListener listener) {
        
    	Trace.d("MainActivity", "showLogoImage(...)");
    	
    	int start = Show ? ( logoImage.getTop() + logoImage.getHeight() ) * -1 : 0;
    	int end = Show ? 0 : ( logoImage.getTop() + logoImage.getHeight() ) * -1;
         
    	TranslateAnimation ta = new TranslateAnimation(0, 0, start, end);
		ta.setDuration(500);
		ta.setFillAfter(true);
		
		ta.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				logoImage.setVisibility(Show ? View.VISIBLE : View.INVISIBLE);
				
				if ( listener != null ) {
					listener.onAnimationEnd(null);	
				}
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
			}


		});
		
		logoImage.startAnimation(ta);

    };
    
    private void infoTouch() {
    	
    	if ( !infoBtn1.isEnabled() ) return;
    	
    	// [self hideLogTextView];
	    
    	if (  openBtn.getVisibility() == View.VISIBLE ) {
    		openBtn.setVisibility(View.INVISIBLE);
    	}

    	logView.setVisibility(View.INVISIBLE);
    	
    	RelativeLayout infoLayout = (RelativeLayout) findViewById(R.id.info_layout);
    	infoBtn1.setEnabled(false);
    	
    	TranslateAnimation ta;
    	
    	final AnimationListener listener = new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				infoBtn1.setEnabled(true);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
				
		};

    	if ( infoLayout == null ) {
        	showActionButtons(false, null);
        	showLogoImage(false, null);	
        	
        	final View InfoView = getLayoutInflater().inflate(R.layout.info_layout, rootLayout, false);
        	
		    rootLayout.addView(InfoView);
			infoLayout = (RelativeLayout) findViewById(R.id.info_layout);
			
			ta = new TranslateAnimation(rootLayout.getWidth()*-1, 0, 0, 0);
			ta.setFillAfter(true);
			ta.setDuration(500);
			ta.setAnimationListener(listener);
			infoLayout.startAnimation(ta);
			
			productHomePage = (TextView) findViewById(R.id.textView3);
			//mfrHomePage = (TextView) findViewById(R.id.textView8);		
			
			productHomePage.setOnClickListener(this);
			//mfrHomePage.setOnClickListener(this);
			
        	
    	} else {
    		
			ta = new TranslateAnimation(0, infoLayout.getWidth()*-1, 0, 0);
			ta.setDuration(500);
			ta.setFillAfter(true);
			ta.setAnimationListener(listener);
			infoLayout.startAnimation(ta);
			rootLayout.removeView(infoLayout);
			
        	showActionButtons(true, null);
        	showLogoImage(true, null);		
    	}    	

    }
    
    /*
    private DCT Connection() {
    	if ( mBoundService != null ) {
    		return mBoundService.Connection;
    	}
    	
    	return null;
    }
    */
    
    private void openTouch() {
    	
        if ( Connection != null
        		&& openBtn.getVisibility() == View.VISIBLE ) {
           
        	
        	openBtnSetEnabled(false);
        	
            setOpeningStatus();
            startWaitingForOpenTimeoutTimer();
            
            if ( openBtnRef == gateBtn ) {
            	Connection.openGate();
            } else if ( openBtnRef == gatewayBtn ) {
            	Connection.openGateway();
            }
        }
    }
    
   private void gateTouch(final Button sender) {
             
        if ( openBtnRef == sender && openBtn.getVisibility() == View.VISIBLE ) {
        	moveOpenButton(sender, true, null);
        } else {
            if ( openBtn.getVisibility() == View.VISIBLE ) {
            	
            	moveOpenButton(sender, true, new AnimationListener() {
			          @Override
			          public void onAnimationStart(Animation animation) {
			          }
			          @Override
			          public void onAnimationEnd(Animation animation) {
			        	  moveOpenButton(sender, false, null);
			          }
		              @Override
			          public void onAnimationRepeat(Animation animation) {
			          }
				
		        });
            	
            } else {
            	 moveOpenButton(sender, false, null);
            }
        }
        
    }
    
    private boolean Connected() {
    	return Connection != null && Connection.isAuthorized();
    }
    
    private void SipError() {
    	
	    if ( sipErrorTimer1 != null ) {
	    	sipErrorTimer1.cancel();
	    }
	    
	    videoBtn.setTag(new java.lang.Integer(0));
	    
	    sipErrorTimer1 = new Timer();
	    sipErrorTimer1.schedule(new sipErrorTimer1Task(), 0, 200);
	    
	    setConnectedStatusWithActInd(false);
        sipTerminate();
    }
    
    class updateTimer1Task extends TimerTask {
		@Override
		public void run() {
			rootLayout.post(new Runnable() {
				@Override
				public void run() {
					updateLogoAndButtonsPosition();
				}
			});
		}
	}
    
    class hideButtonTimer1Task extends TimerTask {
		@Override
		public void run() {
			rootLayout.post(new Runnable() {
				@Override
				public void run() {
					hideOpenButton();
				}
			});
		}
	}
    
    class hideLogTimer1Task extends TimerTask {
		@Override
		public void run() {
			rootLayout.post(new Runnable() {
				@Override
				public void run() {
					hideLog();
				}
			});
		}
	}
    
    class timeoutTimer1Task extends TimerTask {
		@Override
		public void run() {
			rootLayout.post(new Runnable() {
				@Override
				public void run() {
					waitingForOpenTimeout();
				}
			});
		}
	}
    
    class sipErrorTimer1Task extends TimerTask {
    	
		@Override
		public void run() {
			rootLayout.post(new Runnable() {
				@Override
				public void run() {
				
					
				    if ( (Integer)videoBtn.getTag() < 10 ) {
				        
				        Integer i = (Integer)videoBtn.getTag();

				        if ( (i & 1) == 1 ) {
				        	videoBtn.setBackgroundResource(R.drawable.video_w);
				        	voiceBtn.setBackgroundResource(R.drawable.mic_w);
				        } else {
				        	videoBtn.setBackgroundResource(R.drawable.video_r);
				        	voiceBtn.setBackgroundResource(R.drawable.mic_r);
				        }

				        i++;
				        videoBtn.setTag(i);
				        
				    } else {

			        	videoBtn.setBackgroundResource(R.drawable.video_g);
			        	voiceBtn.setBackgroundResource(R.drawable.mic_g);
				        
					    videoBtn.setEnabled(true);
					    voiceBtn.setEnabled(true);
					    
				        if ( sipErrorTimer1 != null ) {
				            sipErrorTimer1.cancel();
				            sipErrorTimer1 = null;
				        };
				    }
				    
				}
			});
		}
	}
    
    class sipTimeoutTimer1Task extends TimerTask {
    	
		@Override
		public void run() {
			rootLayout.post(new Runnable() {
				@Override
				public void run() {
									    
				    videoBtn.setTag(new java.lang.Integer(0));
				    videoBtn.setEnabled(false);
				    voiceBtn.setEnabled(false);
				    
				    SipError();
				}
			});
		}
	}
    
    class audioVideoTimer1Task extends TimerTask {
    	
    	private View _sender;
    	
    	public audioVideoTimer1Task(View sender) {
    		super();
    		_sender = sender;
    	}
    	
		@Override
		public void run() {
			rootLayout.post(new Runnable() {
				@Override
				public void run() {
					
				    String SipServer = Connection == null ? "" : Connection.SipServer();
				    if ( SipServer.length() > 0) {

				        Trace.d(LOGTAG, "SipServer="+SipServer);			      
				        
				        if ( LLP == null || LLP.ActiveCall() == false ) {
				        	sipTimeoutTimer1 = new Timer();
				        	sipTimeoutTimer1.schedule(new sipTimeoutTimer1Task(), 20000);
				        }
				        
				        LpInit();
				        if ( _sender == voiceBtn ) {
				        	Trace.d(LOGTAG, "_sender == voiceBtn");
				        	LLP.SetAudioEnabled(true);
				            if ( LLP.ActiveCall() == true ) {
				      
				            	Trace.d(LOGTAG, "SipConnect");
				            	Connection.SipConnect(LLP.GetAudioEnabled() == true ? true : false, LLP.GetVideoEnabled() == true ? true : false);
				            } else {
				            	Trace.d(LOGTAG, "ActiveCall");
				            }
				        } else {
				        	Trace.d(LOGTAG, "_sender != voiceBtn");
				        	LLP.SetAudioEnabled(false);
				        }
				        
				        Trace.d(LOGTAG, "ActiveCall LLP.GetAudioEnabled()="+LLP.GetAudioEnabled());
				        LLP.SetVideoEnabled(true);
				        //[Linphone resetRetryCounter];
				        Trace.d(LOGTAG, "----->Registered: "+LLP.Registered(GetSipIdent(), SipServer));
				        LLP.Register(GetSipIdent(), SipServer);
				        
				    }
				}
			});
		}
	}
    
    
    private void updateWithDelay() {
		if ( updateTimer1 != null ) {
			updateTimer1.cancel();
			updateTimer1 = null;
		}
		
		updateTimer1 = new Timer();
		updateTimer1.schedule(new updateTimer1Task(), 1000);
    }
    
    private void updateLogoAndButtonsPosition() {
        
    	Trace.d(LOGTAG, "updateLogoAndButtonsPosition");
    	logView.setVisibility(View.INVISIBLE);
    	RelativeLayout infoLayout = (RelativeLayout) findViewById(R.id.info_layout);
    	
        //if ( self.infoView.hidden == NO && self.videoView.hidden == NO ) {
        //    [self updateWithDelay];
        //    [self infoTouch:self.btnInfo];
        //    return;
        // }
        
        if ( infoLayout != null 
        		|| !infoBtn1.isEnabled() ) return;
        
        if ( updateInProgress == true ) {
            updateWithDelay();
            return;
        }
        
        updateInProgress = true;
        
        AnimationListener l = new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
			   updateInProgress = false;
			}
			@Override
			public void onAnimationRepeat(Animation animation) {	
			}
		};
        
        if ( Connected() ) {
            
        	/*
            if ( self.videoView.hidden ) {
                self.logoImage.hidden = NO;
                [self moveLogoWithAnimationFromYposition:self.logoImage.frame.origin.y toYposition:logoYpos1 completion:^(BOOL finished){
                    [self showActionButtons:YES completion:^(BOOL finished){
                        self.logoImage.tag = 0;
                    }];
                }];
            } else {
                self.logoImage.hidden = YES;
                [self showActionButtons:YES completion:^(BOOL finished){
                    self.logoImage.tag = 0;
                }];
            }
            */
        	logoImage.setVisibility(videoLayout.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
        	showActionButtons(true, l);
            
        } else {

        	Trace.d("MainActivity", "updateLogoAndButtonsPosition DISCONNECTED");
        	logoImage.setVisibility(View.VISIBLE);
        	
        	if ( openBtn.getVisibility() == View.VISIBLE ) {
        		openBtn.setVisibility(View.INVISIBLE);
        	}

            
            rootLayout.removeView(infoLayout);
            
            showActionButtons(false, l);
            
            //[self showActionButtons:NO completion:^(BOOL finished){
            //    [self moveLogoWithAnimationFromYposition:self.logoImage.frame.origin.y toYposition:logoYpos0 completion:^(BOOL finished){
            //        self.logoImage.tag = 0;
            //    }];
            //}];
        }
        

    }
    
    private int getImg(int type) {
    	
    	switch(type) {
    	case IMG_GATE_BIG_G:
    		return GateIsOpen ? R.drawable.gate_big_g : R.drawable.gatec_big_g;
    	case IMG_GATE_BIG_HL:
    		return GateIsOpen ? R.drawable.gate_big_hl : R.drawable.gatec_big_hl;
    	case IMG_GATE_BIG_W:
    		return GateIsOpen ? R.drawable.gate_big_w : R.drawable.gatec_big_w;
    	case IMG_GATE_G:
    		return GateIsOpen ? R.drawable.gate_g : R.drawable.gatec_g;
    	case IMG_GATEWAY_BIG_G:
    		return GatewayIsOpen ? R.drawable.gateway_big_g : R.drawable.gatewayc_big_g;
    	case IMG_GATEWAY_BIG_HL:
    		return GatewayIsOpen ? R.drawable.gateway_big_hl : R.drawable.gatewayc_big_hl;
    	case IMG_GATEWAY_G:	
    		return GatewayIsOpen ? R.drawable.gateway_g : R.drawable.gatewayc_g;
    	}

    	
    	return 0;
    }
    
    private void openBtnSetEnabled(Boolean enabled) {
    	
    	int ResID = 0;
    	
    	openBtn.setEnabled(enabled);
    			
    	if ( openBtnRef == gateBtn ) {
    		ResID = enabled ? getImg(IMG_GATE_BIG_G): getImg(IMG_GATE_BIG_HL);
    	} else if ( openBtnRef == gatewayBtn ) {
    		ResID = enabled ? getImg(IMG_GATEWAY_BIG_G) : getImg(IMG_GATEWAY_BIG_HL);
    	}
    	
    	openBtn.setBackgroundResource(ResID);
    		
        
    }
    
    private void moveOpenButton(Button type, final Boolean hide, final AnimationListener listener) {
        
        if ( !gateBtn.isEnabled() || (openBtn.getVisibility() == View.INVISIBLE) == hide ) return;
        
        
        setOpenBtnMargins();
        
        gateBtn.setEnabled(false);
        gatewayBtn.setEnabled(false);
        
        openBtn.setVisibility(View.VISIBLE);
  

        if ( !hide ) {            
            startHideOpenButtonTimer();
            openBtnRef = type;
            openBtnSetEnabled(true);
        }

        final int l = (openBtn.getLeft()+openBtn.getWidth())*-1;

        TranslateAnimation ta = new TranslateAnimation(hide ? 0 : l, hide ? l : 0, 0, 0);
		ta.setDuration(250);
		ta.setFillAfter(true);
		ta.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				openBtn.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
		        gateBtn.setEnabled(true);
		        gatewayBtn.setEnabled(true);
		     
	
		        if ( !hide ) {
		        	openBtn.clearAnimation();
		        }
                
				if ( listener != null ) {
					listener.onAnimationEnd(null);	
				}
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
				
		});
		openBtn.startAnimation(ta);
		
       
    }

    
    private void hideOpenButton() {
        if ( openBtn.getVisibility() == View.VISIBLE )  {
            if ( currentStatusID != STATUS_OPENING ) {
            	moveOpenButton(null, true, null);
                hideButtonTimer1 = null;
            }
        }
    }
    
    private void hideLog() {
    	logView.setVisibility(View.INVISIBLE);
    }
    
    private void startHideOpenButtonTimer() {
    	
		if ( hideButtonTimer1 != null ) {
			hideButtonTimer1.cancel();
			hideButtonTimer1 = null;
		}
		
		hideButtonTimer1 = new Timer();
		hideButtonTimer1.schedule(new hideButtonTimer1Task(), 5000);
    }
    
    private void startWaitingForOpenTimeoutTimer() {
    	
    	stopWaitingForOpenTimeoutTimer();

		
		timeoutTimer1 = new Timer();
		timeoutTimer1.schedule(new timeoutTimer1Task(), 30000);
		
    }

    private void stopWaitingForOpenTimeoutTimer() {
    	
		if ( timeoutTimer1 != null ) {
			timeoutTimer1.cancel();
			timeoutTimer1 = null;
		}
		
    };

    private void waitingForOpenTimeout() {
    	LibDomophone.dEvent Event = (new LibDomophone()).new dEvent();

        Event.Scope = LibDomophone.ES_OPEN;
        Event.Type = LibDomophone.ET_ONEND;
        
        cevent_Event(Event);
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
    
	@Override
	public void onClick(View v) {
		
	
		/*
			
		LpInit();
		LLP.SetLogsOn(1);
		LLP.Register("aaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbba", "46.41.132.7");
		ShowVideoWindow(true);
		LLP.SetVideoEnabled(1);
	
*/
		
		/*
		videoLayout.setVisibility(View.VISIBLE);
		btnsLayout.setVisibility(View.INVISIBLE);
	    updateLogoAndButtonsPosition();
		
    	sipTimeoutTimer1 = new Timer();
    	sipTimeoutTimer1.schedule(new sipTimeoutTimer1Task(), 10000);
    	*/
		
	//	int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_VOICE_CALL);
	//	mAudioManager.setStreamVolume(STREAM_VOICE_CALL, maxVolume, 0);
		
		
		if ( v == videoBtn
			 || v == voiceBtn ) {
			
                long Diff = System.currentTimeMillis() - LastAudioVideoTouch;
		    
			    if ( ( v == videoBtn
			            && ( LLP == null
			                 || LLP.GetVideoEnabled() == false 
			                 || videoLayout.getVisibility() == View.INVISIBLE ) )
			        || ( v == voiceBtn
			             && ( LLP == null
			                  || LLP.GetAudioEnabled() == false ) )) {
			            
			    	if ( LargeScreen ) {
			    		setRequestedOrientation(getScreenOrientation());
			    	}
			    	
			        if ( Connection != null ) {
			            
			        	setConnectedStatusWithActInd(videoLayout.getVisibility() == View.INVISIBLE);
			        
			        	if ( v == voiceBtn ) {
			        		changeBtnImage(voiceBtn, R.drawable.mic_on);
			        		setMicrophoneGain();
			        		Connection.SetSpeakerOn(true);
			            };
			        	
			        	
			        	changeBtnImage(videoBtn, R.drawable.video_on);
			        	
			            if ( Diff >= 4000 ) {
			                Diff = 250;
			            }
			            
			            if ( videoTimer1 != null ) {
			                videoTimer1.cancel();
			            }
			           
			    		videoTimer1 = new Timer();
			    		videoTimer1.schedule(new audioVideoTimer1Task(v), Diff);
			            
			        }
			        
			    } else {
			        Trace.d("MainActivity", "audioVideoTouch:sipDisconnect LLP.GetAudioEnabled()=="+ (LLP != null && LLP.GetAudioEnabled() == true ? "1" : "0"));
			        sipDisconnect();
			    }


			    LastAudioVideoTouch = System.currentTimeMillis();
			    

		} else if ( v == settingsBtn ) {
			((Activity) this).openOptionsMenu();  
		} else if ( v == infoBtn1 ) {
 
			infoTouch();
			
		} else if ( v == gateBtn
		            || v == gatewayBtn ) {
			
			gateTouch((Button)v);
			
		} else if ( v == openBtn ) {
			
			openTouch();
			
			
		} else if ( v == productHomePage 
				|| v == mfrHomePage ) {
			Intent i = new Intent(Intent.ACTION_VIEW, 
		    Uri.parse(v == productHomePage ? "http://www.domophone.eu" : "http://www.acsoftware.pl"));
		    startActivity(i);
		}
		
		
	}
	
	@Override
	public void openOptionsMenu() {

	    Configuration config = getResources().getConfiguration();

	    if((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) 
	            > Configuration.SCREENLAYOUT_SIZE_LARGE) {

	        int originalScreenLayout = config.screenLayout;
	        config.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE;
	        super.openOptionsMenu();
	        config.screenLayout = originalScreenLayout;

	    } else {
	        super.openOptionsMenu();
	    }
	}
	
	private void changeBtnImage(Button btn, int img) {
		btn.setBackgroundResource(img);
	}
	
	private void connectionInit() {
		  
	    //_lastConnectionInit = [NSDate date];
	    sipTerminate();
	    
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
	        Caps = 0;
	        
	        LastSysStateTime = 0;
	        

	        Connection = new DomophoneConnection(_dc_handler);
	        Connection.startWithInitialize(
					prefs.getString("pref_authkey", ""),
					prefs.getString("pref_serialkey", ""),
					ID, IP, PORT, 0);
	        

	    } else {
	    	setNotConnectedStatus();
	    	updateLogoAndButtonsPosition();
	    }
	    
	}

	private void disconnect() {
	    
	    sipTerminate();
	   
		CloseConnection();
	    
	    Caps = 0;
	    updateLogoAndButtonsPosition();
	}
	
	private void cevent_Disconnected() {
		
	    sipTerminate();
	    Caps = 0;

	    if ( Connection != null ) {
	    	setConnectingStatus();
            updateLogoAndButtonsPosition();
	    }
	}
	
	private void cevent_Authorized(LibDomophone.ConnectionSettings cs) {
		
		Caps = 0;
	        
        if ( (cs.Caps & LibDomophone.CAP_AUDIO) > 0 )
            Caps|=ACDEVCAP_AUDIO;
        
        if ( (cs.Caps & LibDomophone.CAP_VIDEO) > 0 )
            Caps|=ACDEVCAP_VIDEO;
        
        if ( (cs.Caps & LibDomophone.CAP_OPEN1) > 0 )
            Caps|=ACDEVCAP_GATEWAY;
        
        if ( (cs.Caps & LibDomophone.CAP_OPEN2) > 0  )
            Caps|=ACDEVCAP_GATE;
 
        if (cs.SerialKey != null && cs.SerialKey.length() > 0 ) {
        	
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        	String Serial = prefs.getString("pref_serialkey", "");
        	if ( Serial.length() == 0 
        		 || !Serial.equals(cs.SerialKey) ) {
        		
        		Editor editor = prefs.edit();
      			editor.putString("pref_serialkey", cs.SerialKey);
      			editor.commit();
        	}
        }
        
        Trace.d("Main", "cevent_Authorized(...)");
		    
		setConnectedStatusWithActInd(false);
	    updateLogoAndButtonsPosition();
	    
	}
	
	private void cevent_Unauthorized() {

	    disconnect();
	    setUnauthorizedStatus();

	}

	private void cevent_VersionError() {
	    
	    disconnect();
	    setVersionErrorStatus();
	    
	}

	private void cevent_Event(LibDomophone.dEvent event) {
	    
	    if ( event.Type == LibDomophone.ET_ONBEGIN
	        && event.Scope == LibDomophone.ES_RING ) {
	        
	    	addLogItem(getResources().getString(R.string.calling), "DOMOPHONE", Boolean.TRUE);
	    	
	    	UserNotification n = new UserNotification(this);
			n.showRingNotification();
	    
	        
	    } else if ( event.Type == LibDomophone.ET_ONBEGIN ) {
	    	
	    	if ( event.Scope == LibDomophone.ES_OPEN ) {
	    		 setOpeningStatus();
	    	}
	       
	        if ( event._Owner == 0 ) {
	            if (  event.Scope == LibDomophone.ES_OPEN
	                  && ( event.Param1 == LibDomophone.ACTION_OPEN1
	                      || event.Param1 == LibDomophone.ACTION_OPEN2 ) ) {
	            	
	            	addLogItem(getResources().getString(event.Param1 == LibDomophone.ACTION_OPEN1 ? R.string.gateway_opening : R.string.gate_opening_closing), event.SenderName, Boolean.TRUE);
	     
	            } else if ( event.Scope == LibDomophone.ES_SIP ) {
	            	
	            	addLogItem(getResources().getString(R.string.audiovideo_started), event.SenderName, Boolean.TRUE);
	            }
	        }
	        
	    } else if ( event.Type == LibDomophone.ET_ONEND
	               && event.Scope == LibDomophone.ES_OPEN ) {
	        
	       et_onend_es_open();
	        
	    } else if ( event.Type == LibDomophone.ET_ONEND
	                && event.Scope == LibDomophone.ES_SIP ) {
	        if ( event._Owner == 1 ) {
	           sipTerminate(); 
	         } else {
	             addLogItem(getResources().getString(R.string.audiovideo_finished), event.SenderName, Boolean.TRUE);
	         }
	    }
	}
	
	private void et_onend_es_open() {
		
        if ( currentStatusID == STATUS_OPENING ) {
            stopWaitingForOpenTimeoutTimer();
        	setConnectedStatusWithActInd(false);
        }
        
        startHideOpenButtonTimer();
        openBtnSetEnabled(true); 
	}
	
	private void cevent_Locked(LibDomophone.dLockEvent event) {
		
		addLogItem(getResources().getString(R.string.locked_by), event.SenderName, Boolean.TRUE);
	    et_onend_es_open();
	    //sipTerminateWithBySendAction(false);
	}
	
	private void cevent_SysState(LibDomophone.dSysState state) {
		
		int status = state.state;
		LastSysStateTime = System.currentTimeMillis();

	    if ( currentStatusID == STATUS_OPENING
	            && (status & LibDomophone.SYS_STATE_OPENING1) != LibDomophone.SYS_STATE_OPENING1
	            && (status & LibDomophone.SYS_STATE_OPENING2) != LibDomophone.SYS_STATE_OPENING2
	            && (status & LibDomophone.SYS_STATE_OPENING3) != LibDomophone.SYS_STATE_OPENING3 ) {
	            et_onend_es_open();
	        }
	    
	    Boolean GOPEN = ( status & LibDomophone.SYS_STATE_GATEISCLOSED ) != LibDomophone.SYS_STATE_GATEISCLOSED;
	    Boolean GWOPEN = ( status & LibDomophone.SYS_STATE_GATEWAYISCLOSED ) != LibDomophone.SYS_STATE_GATEWAYISCLOSED;
	    
	    if ( GateIsOpen != GOPEN
	    	 || GatewayIsOpen != GWOPEN ) {
	    	
	    	GateIsOpen = GOPEN;
	    	GatewayIsOpen = GWOPEN;
	    	
	    	int v = openBtn.getVisibility();
	    			
	    	openBtnSetEnabled(openBtn.isEnabled());
	    	gateBtn.setBackgroundResource(getImg(IMG_GATE_G));
	    	gatewayBtn.setBackgroundResource(getImg(IMG_GATEWAY_G));
	    	
	    	openBtn.setVisibility(v);
	    		
	    }

	    if ( state._proxy_src == false ) {
	    	
	   	    if ( state.firmware_version < 4
	   		     && fv_warning == false  ) {
		      	fv_warning = true;
		    	addLogItem(getResources().getString(R.string.firmware_warning), "", Boolean.TRUE);
		    	Trace.d(LOGTAG, "FirmwareVersion:"+state.firmware_version);
	   	    }
	   	 
		    if ( state.firmware_version >= 4
			    	 && (state.state & LibDomophone.SYS_STATE_PROXYREGISTERED) != LibDomophone.SYS_STATE_PROXYREGISTERED
			    		    && System.currentTimeMillis() - proxy_warning > 60000 ) {
			    	proxy_warning = System.currentTimeMillis();
			    	addLogItem(getResources().getString(R.string.proxy_warning), "", Boolean.TRUE);
		    }
		    
	    }
	    
	    
	}

	private void cevent_RegisterPushID() {
		
		if ( Connected() ) {
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	        String RegID = prefs.getString("push_reg_id_key", null);
	        
	        Connection.RegisterPushID(RegID == null ? "" : RegID);
		}		

	}
	
	static public  Uri getNotifySoundUri(Context context) {
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
		if ( prefs.getBoolean("silent_mode", false) == true ) {
			return null;
		}
		
        String SID = prefs.getString("notify_sound", null);
        
        if ( SID.equals("0") ) {
        	SID = prefs.getString("notify_system_sound", null);
        	
        	if ( SID != null && SID.length() != 0 ) {        		
        		return Uri.parse(SID);
        	}
        	
        	return null;
        };

		return Uri.parse("android.resource://" + context.getApplicationContext().getPackageName() + "/" + context.getResources().getIdentifier("ring"+SID, "raw", context.getPackageName()));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		setSpeakerOn();
		
		if ( key.equals("pref_ip")
			|| key.equals("pref_authkey")
			|| key.equals("pref_serialkey")) {

			connectionInit();

		} else if ( key.equals("notify_sound") ) {
		
			Uri file = getNotifySoundUri(this);
			
			
			if ( file != null ) {
				MediaPlayer mPlayer = MediaPlayer.create(MainActivity.this, file);
				if ( mPlayer != null ) {
					mPlayer.start();
				}
				
			}

		} else if ( key.equals("bg_mode") ) {
			StartStopROService();
		}
 		
	}
	
	public synchronized void LpInit() { 
		if ( LLP == null 
			 && Connection != null ) {
			synchronized (_lock) {
				if ( LLP == null ) {
					LLP = new LibLP(_lp_handler);

					LLP.Initialize(true, null, null);
					LLP.setVideoWindowId(androidVideoWindowImpl);	
					
					LLP.EnableEchoLimiter(true);
				    LLP.EnableConfortNoise(false);
					LLP.EnableEchoCancellation(false);
				}
			}
		}
	}

	public String GetSipIdent() {
	    
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    
	    return prefs.getString("pref_id", "").replace("-", "") + "-" + prefs.getString("pref_serialkey", "").replace("-", "");
	}
	 
	public void SipRegistered() {
		
		if ( Connection != null 
			 && LLP != null 
			 && LLP.ActiveCall() == false ) {
			setConnectedStatusWithActInd(true);
			Trace.d(LOGTAG, "SipRegistered");
			Connection.SipConnect(LLP.GetAudioEnabled() == true ? true : false, LLP.GetVideoEnabled() == true ? true : false);
		}
		
		
	}
	
	public int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
          if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          } else {
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          }
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
          if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          } else {
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          }
        }
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
      }
	
	private Boolean ShowVideoWindow(Boolean Show) {
		
		Boolean v = videoLayout.getVisibility() == View.VISIBLE;
		
	    if ( Show) {
	    
	    	   
	    	if ( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
	    	

	    		
	    	   int t = (int)(20 * getResources().getDisplayMetrics().density);
	    	   int h = rootLayout.getHeight() - openBtn.getHeight() - getStatusBarHeight() - (int)(50 * getResources().getDisplayMetrics().density);
	    	   int w = (int)(h * 1.25);
	    	   int l = getRightMargin() + voiceBtn.getWidth();
	    	   
	    	   setMargins(mVideoView, l, t, rootLayout.getWidth() - l - w,  rootLayout.getHeight() - t - h );
	    
	    	} else {
	    		int x = (int)((LargeScreen ? 50 : 15) * getResources().getDisplayMetrics().density);
	    		int y = getBtnsYPos(true) * 2 + voiceBtn.getHeight();
	    		setMargins(mVideoView, x, y, x, rootLayout.getHeight() - y - (int)((rootLayout.getWidth() - x * 2) * 0.85) );
	    	}
	    	
	    	gateBtn.setVisibility(View.INVISIBLE);
	    	gatewayBtn.setVisibility(View.INVISIBLE);
	    	videoBtn.setVisibility(View.INVISIBLE);
	    	voiceBtn.setVisibility(View.INVISIBLE);
	    	openBtn.setVisibility(View.INVISIBLE);
	    	infoBtn1.setVisibility(View.INVISIBLE);
	    	logView.setVisibility(View.INVISIBLE);
	    	settingsBtn.setVisibility(View.INVISIBLE);
	        videoLayout.setVisibility(View.VISIBLE);
	        logoImage.setVisibility(View.INVISIBLE);
	        
	        
	        
	    } else {
	    	
	    	videoLayout.setVisibility(View.INVISIBLE);
	    	settingsBtn.setVisibility(View.VISIBLE);	
	    	infoBtn1.setVisibility(View.VISIBLE);
	    	logoImage.setVisibility(View.VISIBLE);
	        
	        if ( openBtn.getVisibility() == View.VISIBLE ) {
	        	openBtn.setVisibility(View.INVISIBLE);
	        }
	        

	    }
	    
	    
	    return v != Show;
	}
	
	private void stopSipTimeoutTimer() {
		
		if ( sipTimeoutTimer1 != null ) {
			sipTimeoutTimer1.cancel();
			sipTimeoutTimer1 = null;
		}
		
	}
	
	private void sipTerminateWithBySendAction(Boolean SendAction) {
	    
	    stopSipTimeoutTimer();
		sipDisconnect(true, SendAction);
	    
	    if ( LLP != null ) {
	    	LLP.Clean();
	    };
	    

	    et_onend_es_open();
	    
        if ( LargeScreen ) {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
	}

	private void sipTerminate() {
	    sipTerminateWithBySendAction(true);
	}

	private void sipDisconnect(Boolean wt, Boolean SendAction) {
	    
	    if ( videoTimer1 != null ) {
	        videoTimer1.cancel();
	        videoTimer1 = null;
	    }
	    
	    changeBtnImage(voiceBtn, R.drawable.mic_g);
	    sipVideoStopped();
	    
	    if ( SendAction && Connection != null ) {
	        //NSLog(@"sipDisconnectWithouthTerminate:SipDisconnect");
	    	Connection.SipDisconnect();
	    } else if ( wt ==false ) {
	        sipTerminate();
	    };
	}

	private void sipDisconnect() {
		sipDisconnect(false, true);
	}
	
	private void setMicrophoneGain() {
	    if ( LLP != null ) {
	     	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	     	LLP.SetMicrophoneGain(prefs.getFloat("mic_gain", 10));
	    }
	}
	
	
	private void setPlaybackGain() {
	    if ( LLP != null ) {
	     	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	     	LLP.SetPlaybackGain(prefs.getFloat("playback_gain", 2));
	    }
	}
	
	
	

	private void delayedSpeakerOn() {
		
        TimerTask lTask = new TimerTask() {
            @Override
            public void run() {
            	setSpeakerOn();      
            }
        };
        
        Timer sTimer = new Timer("SPON");
        sTimer.schedule(lTask, 1000);
	}
	
	private void sipCallStarted() {
		
	    stopSipTimeoutTimer();
	    if ( LLP == null || LLP.GetVideoEnabled() == false || videoLayout.getVisibility() == View.VISIBLE ) {
	        setConnectedStatusWithActInd(false);
	    };
	    
	    if ( LLP != null && LLP.GetAudioEnabled() == true ) {
	    	setMicrophoneGain();
	    }

     	updateLogoAndButtonsPosition();
	    setSpeakerOn();
	    delayedSpeakerOn();
	}

	private void sipVideoStarted() {
	    
		if ( ShowVideoWindow(true) ) {
			btnsLayout.setVisibility(View.INVISIBLE);
		}
	    
	    if ( currentStatusID == STATUS_WAITING
	        || currentStatusID == STATUS_CONNECTED) {
	       setConnectedStatusWithActInd(false);
	    }
	    
	    Trace.d(LOGTAG, "------- > VIDEO STARTED < --------");

	    if ( LLP != null ) {
	    	LLP.setVideoWindowId(androidVideoWindowImpl);
	    }
	    
	    
		updateLogoAndButtonsPosition();
		setSpeakerOn();
		delayedSpeakerOn();
		    
	}

	private void sipVideoStopped() {
	
		Trace.d(LOGTAG, "------- > VIDEO STOPPED < --------");
		
		changeBtnImage(videoBtn, R.drawable.video_g);
		
	    if ( ShowVideoWindow(false) ) {
	    	btnsLayout.setVisibility(View.INVISIBLE);
	    }
	    
	    updateLogoAndButtonsPosition();
	}
	
	private void setSpeakerOn() {
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		enableSpeaker(prefs.getBoolean("speaker_on", true));
	}
	
	private static boolean isGTP1000() {return Build.DEVICE.startsWith("GT-P1000");};
	
	private static boolean isGT9000() {return Build.DEVICE.startsWith("GT-I9000");};
	
	private static boolean isSC02B() {return Build.DEVICE.startsWith("SC-02B");};
	
	private static boolean isSGHI896() {return Build.DEVICE.startsWith("SGH-I896");};
	
	private static final boolean isSPHD700() {return Build.DEVICE.startsWith("SPH-D700");};
	
	public static boolean isGalaxyTab() {
		return isGTP1000();
	}
	private static boolean isGalaxyS() {
		return isGT9000() || isSC02B() || isSGHI896() || isSPHD700();
	}

	public static boolean isGalaxySOrTab() {
		return isGalaxyS() || isGalaxyTab();
	}
	
	public static boolean needGalaxySAudioHack() {
		return isGalaxySOrTab() && !isSC02B();
	}
	
	private void setAudioModeIncallForGalaxyS() {
		mAudioManager.setMode(android.media.AudioManager.MODE_IN_CALL);
	}
	
	public void routeAudioToSpeakerHelper(boolean speakerOn) {

		if (needGalaxySAudioHack())
			setAudioModeIncallForGalaxyS();
		mAudioManager.setSpeakerphoneOn(speakerOn);
	}
	
	public void enableSpeaker(boolean value) {

		setPlaybackGain();
		setMicrophoneGain();
		
		if (LLP != null 
				&& LLP.StreamsRunning()
				&& needGalaxySAudioHack()) {
			
			LLP.forceSpeakerState(value);

		} else {
			routeAudioToSpeakerHelper(value);
		}
	}
    
    public void initPushNotificationService(Context context) {
    	

    	_gcm_handler = new Handler() {
    		@Override
    		public void handleMessage(Message msg) {

    			switch(msg.what) {
    				case GCMService.GCM_ONREGISTER:
    				case GCMService.GCM_ONUNREGISTER:
    					cevent_RegisterPushID();
    					break;
    				case GCMService.GCM_REGISTERERROR:

    					Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.push_register_error)+msg.obj, Toast.LENGTH_LONG);
    					toast.show();
    					
    					break;
    					
    			}
    			super.handleMessage(msg);
    		}

    	};
    	
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        try {
                // Starting the push notification service
                GCMRegistrar.checkDevice(context);
                GCMRegistrar.checkManifest(context);
                final String regId = GCMRegistrar.getRegistrationId(context);
                String newPushSenderID = "878000791682";
                String currentPushSenderID = prefs.getString("push_sender_id_key", null);
                if (regId.equals("") || currentPushSenderID == null || !currentPushSenderID.equals(newPushSenderID)) {
                        GCMRegistrar.register(context, newPushSenderID);

                        Trace.d(LOGTAG, "Push Notification : storing current sender id = " + newPushSenderID);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("push_sender_id_key", newPushSenderID);

                        editor.commit();
                } else {
                        Trace.d(LOGTAG, "Push Notification : already registered with id = " + regId);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("push_reg_id_key", regId);
                        editor.commit();
                }
        } catch (java.lang.UnsupportedOperationException e) {
                Trace.d(LOGTAG, "Push Notification not activated: "+e.getMessage());
        }
}

	
}
