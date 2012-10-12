package myroad.myroadpack.namespace;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Xml;

public class MyRoadService extends Service {
	
//	final static String MY_FILEDATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";					
	
	private String phoneNo = MRDefaults.DEFAULT_PHONE;	
	private int iSecs = MRDefaults.DEFAUIT_SECS;
	private int iMetres = MRDefaults.DEFAUIT_METRES; 

	private SharedPreferences preferences;
	OnSharedPreferenceChangeListener prefsListener;	
	
	String strUrl = MRDefaults.baseURL + "/track.php";

	String sretvalue = "";
	String sDetectedCoords = "";
	String sDetectedCoordsDateTime = "";
	String lastCoordStatus = "";
	String sNetStatus = "";
	
	String lang = "";
		
	LocationManager mlocManager;
	
	boolean statusOfGPS = false;
	boolean statusOfNetwork = false;
	boolean statusOfPassive = false;	
	
	boolean isSenderWorkNow = false;
	
    BroadcastReceiver smsReceiver1 =  null;
    BroadcastReceiver smsReceiver2 = null;
	
	String sendingMethod = MRDefaults.DEFAULT_METHOD;
	String optSendingMethod = MRDefaults.DEFAULT_METHOD;
	String lastOksendingMethod = MRDefaults.DEFAULT_METHOD;

	Double latitude  = 0.0;
	Double longitude = 0.0;
	Double altitude  = 0.0;
	String latString = "";
	String lonString = "";
	String altString = "";	
	
	String us = "";
	String pw = "";	
    String tr = "";	
    
    Location loc = null;
    Location prevLoc = null;
    
	long lPrevMillis = 0;
	
	private final BlockingQueue<CoordPoint> queue = new LinkedBlockingQueue<CoordPoint>(MRDefaults.MAX_QUEUE);
    private final BlockingQueue<CoordPoint> pList = new LinkedBlockingQueue<CoordPoint>(MRDefaults.MAX_QUEUE2);	

	Messenger messenger = null;
	
    private IntentFilter mNetworkStateChangedFilter;       
    public BroadcastReceiver receiver;		           
    private SmsManager smsManager;        
    
    int errno = 0;
    private static int countAll = 0;
    private static int countAllOK = 0;
    
    String storageDirectory = "";        
    String extStorageDirectory = "";
    
    boolean isUseExtStorageDirectory = false;
    
    String queueFileName = "";
	String kmlFileName = "";    
	String gpxFileName = "";	
        
    private LocationListener locationListener = null;
//    private LocationListener networkListener = null;
    
	UploadCoordsTask task = null;	           
    boolean isIamVisible = true;	
	
	private NotificationManager mNM = null;
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();	
    private int NOTIFICATION = 111;	    
   
    int mValue = 0;
            
    static class IncomingHandler extends Handler {    
    	private final WeakReference<MyRoadService> mrs;
    	
    	IncomingHandler(MyRoadService service) {
    		mrs = new WeakReference<MyRoadService>(service);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
        	MyRoadService serv = mrs.get();
        	if(serv != null) {
        		serv.handleMessage(msg);
        	}
        }        	
    }
    
    IncomingHandler inh = new IncomingHandler(this);    
    final Messenger mMessenger = new Messenger(inh);    

    public void handleMessage(Message msg) {
    	Log.d(MRDefaults.LOGTAG, getClass().getName() + msg.toString());
        switch (msg.what) {
            case MRDefaults.MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MRDefaults.MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MRDefaults.MSG_SET_VALUE:                   
                Log.d(MRDefaults.LOGTAG, "msg.arg1 = " + msg.arg1);
                                    
                if(msg.arg1 == MRDefaults.MSG_NAME_LAST_STATE) {
                	// надо добавлять именованную точку
                	
/*
 	        	    bundle.putString(MRDefaults.STR_POINTNAME,    nm);
	        	    bundle.putString(MRDefaults.STR_POINTDESC,    de);
	        	    bundle.putString(MRDefaults.STR_POINTNOLINE,  (noline?"NOLINE":""));	        	    

 */                    	
                	Bundle data = msg.getData();	                		                	
                	String nm = data.getString(MRDefaults.STR_POINTNAME);
                	String de = data.getString(MRDefaults.STR_POINTDESC);
                	String nl = data.getString(MRDefaults.STR_POINTNOLINE);

					GetGPSStatus(true, true, nm, de, ("noline".equals(nl)?true:false), false);					
					SendMessage(MRDefaults.SERVMSG_LAST_STATE, errno);                    	
                }
                
                /*
                if(msg.arg1 == MRDefaults.MSG_GET_LAST_STATE) {
                	
                }
                */
                
                break;
            default:
                //super.handleMessage(msg);
                break;
        }
    }        
                      	
    @Override
    public void onCreate(){
    	super.onCreate();
    	Log.v(MRDefaults.LOGTAG, "Service.onCreate()");
    	
    	preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	
		lang = preferences.getString("lang", "default");	
        if (lang.equals("default")) {
        	lang = getResources().getConfiguration().locale.getCountry();
        }
        if(lang.length()==0) lang = "en";
    	    	
    	loadSettings(1);
    	
    	queueFileName = MRDefaults.getQueueFileName(GetUser(), GetTrack(), "xml");
    	kmlFileName   = MRDefaults.getQueueFileName(GetUser(), GetTrack(), "kml");
    	gpxFileName   = MRDefaults.getQueueFileName(GetUser(), GetTrack(), "gpx");
  
    	mNetworkStateChangedFilter = new IntentFilter();
    	mNetworkStateChangedFilter.addAction(Intent. ACTION_TIME_TICK);		
    	mNetworkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);		  
	
    	mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	smsManager = SmsManager.getDefault();
	
    	mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);    	
    	
    	// Display a notification about us starting.  We put an icon in the status bar.
   		//showNotification();	
	
    	// if(LoadQueue()) {
    	
    	//inh.postAtTime(r, uptimeMillis)
    	
    	getStorageDirectory();
    	
		//Log.v(MRDefaults.LOGTAG, "Sending queue loaded OK");
		if(queue.size()>0) {
		//tView4.setText(tV4Prefix + ": " + queue.size() + " points in queue "+(queue.size()>0?"loaded":""));
			// надо отправлять сообщение для изменения визуального состояния активити
		} else {
//			LoadQueue();
		}
		//}		
				
		LoadQueue();
					
		receiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		    	
				Log.d(MRDefaults.LOGTAG, getClass().getName() + " !!!Action: " + intent.getAction());
				
				if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
					
				    NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				    String mTypeName = info.getTypeName();
				    String mSubtypeName = info.getSubtypeName();
				    boolean mAvailable = info.isAvailable();
				    
				    Log.d(MRDefaults.LOGTAG, getClass().getName() + "" +getString(R.string.network_type) + ": " + 
				    		mTypeName + 
						", " + mSubtypeName + (mAvailable?(""+getString(R.string.available)):(""+
								getString(R.string.not_available))));
				    
				    sNetStatus = (mTypeName.length()>12?(mTypeName.substring(0, 9)+"..."):mTypeName);
				    
				    if(mSubtypeName.length()>0) {
				    	sNetStatus += "(" + 
				    	(mSubtypeName.length()>10?(mSubtypeName.substring(0, 7)+"..."):mSubtypeName)+
				    	")";
				    }
				    sNetStatus += ", " + (mAvailable?(""+getString(R.string.available)):(""+
				    		getString(R.string.not_available))); 				    
					//tView5.setText(tV5Prefix + ": " + sNetStatus);			    			    			    							
				} 														    
		
				if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) { 		
					
						Log.d(MRDefaults.LOGTAG, getClass().getName() + " isSenderWorkNow [" + 
								MRDefaults.GenMyDate(MRDefaults.MY_DATE_FORMAT)+"] = " + isSenderWorkNow);
						
						Log.d(MRDefaults.LOGTAG, getClass().getName() + " isIamVisible     = " + isIamVisible);
						//Log.d(MRDefaults.LOGTAG, getClass().getName() + " eText1.hasFocus = " + eText1.hasFocus());			
						//Log.d(MRDefaults.LOGTAG, getClass().getName() + " eText1.hasSelection = " + eText1.hasSelection());					
						//Log.d(MRDefaults.LOGTAG, getClass().getName() + " eText2.hasFocus = " + eText2.hasFocus());					
						
						SendMessage(MRDefaults.SERVMSG_TIME_TICK, countAllOK);
												
						//if(!isSenderWorkNow) {

							try {
								if(task!=null) {
									Log.d(MRDefaults.LOGTAG, getClass().getName() + " Prev task status = " + 
											task.getStatus());
									
									if(task.getStatus() != AsyncTask.Status.RUNNING)
										isSenderWorkNow = false;
								}
								
								if(!isSenderWorkNow) {
									if(task!=null) {																			
										sretvalue = task.sretvalue;
										errno = task.errno;
										countAllOK += task.countAll;
										countAll += task.countAll;
										
										if(sretvalue.length()>0) {
											Log.e(MRDefaults.LOGTAG, "task res = " + sretvalue);
											// надо отправлять сообщение нотификацией!
											// при клике запускается MyRoad activity
										}
										
										// переключаем меняем метод отправки если errno>0 - если были ошибки										
										if(optSendingMethod.equals("auto") && errno>0) {
											if(sendingMethod.equals("HTTP")) { // переключаем метод отправки если она завершилась неудачно
												Log.v(MRDefaults.LOGTAG, "Sending method changed from HTTP to SMS");
												sendingMethod = "SMS";
											} else {
												Log.v(MRDefaults.LOGTAG, "Sending method changed from SMS to HTTP");					
												sendingMethod = "HTTP";
											}
										} else {
											sendingMethod = optSendingMethod; 
										}										
																				
										// здесь надо посылать сообщение в activity
										SendMessage(MRDefaults.SERVMSG_QUEUE_WAS_SENT, errno);																												
									}								
																	
							    	task = new UploadCoordsTask(GetUser(), sendingMethod, phoneNo,
								    			queue, getBaseContext(), smsManager, lang);    									
								
									if(task!=null) {
										
										Log.d(MRDefaults.LOGTAG, getClass().getName() + " Sender task status = " + 
												task.getStatus());
																		
										task.execute(new String[] { strUrl  });	
										isSenderWorkNow = true;																				
										
										SaveKml();
										SaveGpx();
									} 													
								}
							} catch(IllegalStateException ex) {
								Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + ex.getMessage());
								ex.printStackTrace();											
							} catch (RuntimeException e) {
								Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
								e.printStackTrace();														
							}catch (Exception e2) {
								Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e2.getMessage());
								e2.printStackTrace();														
							}								
						}						
					//}
				}				
		    };
		    		    
		    //---when the SMS has been sent---
		    smsReceiver1 = new BroadcastReceiver(){
		        @Override
		        public void onReceive(Context arg0, Intent arg1) {
		        	String sign = "";
		        	CoordPoint cpn = null;
		        	if(arg1.hasExtra("myroad.myroadpack.namespace." + MRDefaults.MR_SMS_SIGN)) {
		        		sign = arg1.getExtras().getString("myroad.myroadpack.namespace." + 
		        		MRDefaults.MR_SMS_SIGN);

		        		String key = "myroad.myroadpack.namespace." + MRDefaults.MR_COORD_POINT; 
			        	if(arg1.hasExtra(key)) {
			        			cpn = (CoordPoint) arg1.getExtras().getParcelable(key);
			        						        			
			        			Log.d(MRDefaults.LOGTAG, "cpn = " + cpn.getSMSBody());
			        	}
		        	}
		            switch (getResultCode())
		            {
		                case Activity.RESULT_OK:			                	
		                	Log.v(MRDefaults.LOGTAG, "SMS sent OK: " + sign + "");
		                	
		                    break;
		                default:
		                	Log.e(MRDefaults.LOGTAG, "SMS not sent with code: " + getResultCode()+" " + sign);
							sretvalue = "Error: " + "SMS not delivered with code";
							errno++;
		                    break;                        
		            }
											            					            
		        }
		    };
		    
		    registerReceiver(smsReceiver1, new IntentFilter(MRDefaults.SENT));					    
		    
		    //---when the SMS has been delivered---
		    smsReceiver2 = new BroadcastReceiver() {
		        @Override
		        public void onReceive(Context arg0, Intent arg1) {
		        	String sign = "";
		        	CoordPoint cpn = null;
		        	if(arg1.hasExtra("myroad.myroadpack.namespace." + MRDefaults.MR_SMS_SIGN)) {
		        		sign = arg1.getExtras().getString(MRDefaults.MR_SMS_SIGN);
		        		//"myroad.myroadpack.namespace."
			        	if(arg1.hasExtra("myroad.myroadpack.namespace." + MRDefaults.MR_SMS_SER)) {
			        			cpn = (CoordPoint) arg1.getExtras().getSerializable(
			        					"myroad.myroadpack.namespace." + MRDefaults.MR_SMS_SER);
			        			
			        			Log.d(MRDefaults.LOGTAG, "cpn = " + cpn.getSMSBody());
			        	}
		        	}		        	
		            switch (getResultCode())
		            {
		                case Activity.RESULT_OK:
		                	
		                    queue.remove(cpn);
		                    countAllOK ++;
		                	Log.v(MRDefaults.LOGTAG, "SMS delivered OK: " + sign + "");				                	

		                    break;
		                default:
		                	countAll ++;
		                	Log.e(MRDefaults.LOGTAG, "SMS not delivered with code: " + getResultCode()+" " + sign);
							sretvalue = "Error: " + "SMS not delivered with code";
							errno++;
		                    break;                        
		            }						            
		        }
		    };
		    
		    registerReceiver(smsReceiver2, new IntentFilter(MRDefaults.DELIVERED));
		    		    		
		    		
		registerReceiver(receiver, mNetworkStateChangedFilter);
		
		if(locationListener==null) {		
			locationListener = new LocationListener() 
			{ 
				public void onLocationChanged(Location new_loc) {					
						
					boolean isAccept = false;
					if(statusOfGPS) {
						if(LocationManager.GPS_PROVIDER.equals(loc.getProvider())) {
							isAccept = true;						
						}
					} else {
						isAccept = true;
					}
					
					if(new_loc!=null) {
						loc = new_loc;						
					}
					double distance = 0.0;
					
					if(isAccept) { // тут проверяем на таймаут и дистанцию
				    	//distanceTo(Location dest)    
						if(prevLoc!=null) {
							if(loc!=null) {
								distance = loc.distanceTo(prevLoc);
								if((Double.compare(distance, iMetres*1.0) >=0 ) || 
										((SystemClock.elapsedRealtime() - lPrevMillis) > (iSecs*1000))) {
									// дистанция больше
									isAccept = true;
								} else {
									isAccept = false;
								}
							}							
						} else {
							if(loc!=null) { // первая точка
								isAccept = true;
							}							
						}						
					}
					
					if(isAccept) {											
						
						latitude  = new_loc.getLatitude();
						longitude = new_loc.getLongitude();
						altitude  = new_loc.getAltitude();
						
						sDetectedCoordsDateTime = MRDefaults.GenMyDate(MRDefaults.MY_DATE_FORMAT);
						
						Log.d(MRDefaults.LOGTAG, "Provider name:" + new_loc.getProvider());
	
						GetGPSStatus(true, false, "", "", false, true);
						SendMessage(MRDefaults.SERVMSG_LAST_STATE, errno);
						
						prevLoc = loc;
						lPrevMillis = SystemClock.elapsedRealtime();
					}
				}
				
				public void onProviderEnabled(String provider) {				
					if(LocationManager.GPS_PROVIDER.equals(provider))
						statusOfGPS = mlocManager.isProviderEnabled(provider);
					if(LocationManager.NETWORK_PROVIDER.equals(provider))
						statusOfNetwork = mlocManager.isProviderEnabled(provider);				
									
					Log.d(MRDefaults.LOGTAG, "Provider name: " + provider + ", statusOfGPS: " + statusOfGPS + 
							", statusOfNetwork: " + statusOfNetwork);				
					
					GetGPSStatus(true, false, "", "", false, false);
					SendMessage(MRDefaults.SERVMSG_LAST_STATE, errno);
					
					//if (IsGPSOn()) {
					//}
				}
				
				public void onProviderDisabled(String provider) {
					if(LocationManager.GPS_PROVIDER.equals(provider))
						statusOfGPS = mlocManager.isProviderEnabled(provider);				
					if(LocationManager.NETWORK_PROVIDER.equals(provider))
						statusOfNetwork = mlocManager.isProviderEnabled(provider);				
					
					Log.d(MRDefaults.LOGTAG, "Provider name: " + provider + ", statusOfGPS: " + statusOfGPS + 
							", statusOfNetwork: " + statusOfNetwork);				

					GetGPSStatus((statusOfGPS||statusOfNetwork)?true:false, false, "", "", false, false);
					SendMessage(MRDefaults.SERVMSG_LAST_STATE, errno);
					
					//if (!IsGPSOn()) {		
					//}
				}			

				public void onStatusChanged(String provider, int status, Bundle extras) {
					Log.d(MRDefaults.LOGTAG, "Provider: " + provider + " status changed: " + status);
					
					boolean isChanged = false;
		            switch (status) {
			            case LocationProvider.AVAILABLE:
			            	if(LocationManager.GPS_PROVIDER.equals(provider)) {
			            		if(!statusOfGPS) {
			            			statusOfGPS = true;
			            			isChanged = true;
			            		}
			            	} else {
			            		if(!statusOfNetwork) {
			            			statusOfNetwork = true;
			            			isChanged = true;
			            		}		            		
			            	}
			                break;
			            case LocationProvider.OUT_OF_SERVICE:
			            	if(LocationManager.GPS_PROVIDER.equals(provider)) {
			            		if(statusOfGPS) {
			            			statusOfGPS = false;
			            			isChanged = true;
			            		}
			            	} else {
			            		if(statusOfNetwork) {
			            			statusOfNetwork = false;
			            			isChanged = true;
			            		}		            				            		
			            	}
			                break;
			            case LocationProvider.TEMPORARILY_UNAVAILABLE:
			            	if(LocationManager.GPS_PROVIDER.equals(provider)) {
			            		if(statusOfGPS) {
			            			statusOfGPS = false;
			            			isChanged = true;
			            		}			            		
			            	} else {
			            		if(statusOfNetwork) {
			            			statusOfNetwork = false;
			            			isChanged = true;
			            		}		            				            				            		
			            	}
			                break;
		            }															
		            
		            if(isChanged) {
		            	GetGPSStatus(false /*true*/, false, "", "", false, false);
						SendMessage(MRDefaults.SERVMSG_LAST_STATE, errno);
		            }
				}			
			};								
			}
		
		showNotification();
    }
	
    @Override
    public void onDestroy() {
    	Log.v(MRDefaults.LOGTAG, "Service.onDestroy()");
    	
    	getStorageDirectory();
    	
		SaveKml();
		SaveGpx();	
    	
    	if(queue.size()>0) { // тут будем сохранять очередь!
    		SaveQueue();
    	}
    	
    	if(mNM!=null)
    	mNM.cancel(NOTIFICATION);
    	
		if(locationListener!=null) {
			mlocManager.removeUpdates(locationListener);
		}
		try {
			if(receiver!=null) {		
				unregisterReceiver(receiver);
			}						
			if(smsReceiver1!=null) {
				unregisterReceiver(smsReceiver1);
			}
			if(smsReceiver2!=null) {
				unregisterReceiver(smsReceiver2);
			}						
		} catch (Exception e) {
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();	        
	    }		
		
    	super.onDestroy();		
    }
    
    @Override
    public IBinder onBind(Intent intent) {
//    	return mBinder;
        return mMessenger.getBinder();    	
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(MRDefaults.LOGTAG, "LocalService " + "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        
        if(locationListener!=null) {
			Log.d(MRDefaults.LOGTAG, "mlocManager.requestLocationUpdates");
			
			mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 
					0, locationListener);		
			
			mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 
					0, locationListener);					
			/*
			mlocManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, (iSecs*1000), 
					iMetres, locationListener);
					*/				
        	
        }
        return START_NOT_STICKY;
    }   
    
    private void showNotification() {    	
        // In this sample, we'll use the same text for the ticker and the expanded notification
        
        CharSequence text  = "My-road service started"; //"getText(111);
        CharSequence title = "My-road ";

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_stat, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification

        //PendingIntent.getBroadcast(context, requestCode, intent, flags)
        
        Intent intent = new Intent(this, MyRoadActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT & 
        		Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);        
        
        //Intent intent = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.setAction(Long.toString(System.currentTimeMillis()));
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent,
                 PendingIntent.FLAG_UPDATE_CURRENT );
        
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, title, text, contentIntent);       
        
        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(NOTIFICATION, notification);                    	    	
    }   
    
public void sendCoords(boolean isManualParam, String snm, String sde, boolean noline) {
		
		boolean bISAautosend = preferences.getBoolean("autosend", true);
		Log.d(MRDefaults.LOGTAG, "sautosend = " + bISAautosend);
		 
		String sdt = MRDefaults.GenMyDate(MRDefaults.MY_DATE_FORMAT);
		
		String sla = this.latString;
		String slo = this.lonString;
		//String sal = this.altString;
		String sal = "";		
		String str = "";
				
		try {
			us = preferences.getString("username", "");
		} catch (Exception e11) {
			Log.e(MRDefaults.LOGTAG, "Exception: " + e11.getMessage());
			e11.printStackTrace();
			us = "";
		}

		try {
			pw = preferences.getString("password", "");
		} catch (Exception e22) {
			Log.e(MRDefaults.LOGTAG, "Exception: " + e22.getMessage());
			e22.printStackTrace();
			pw = "";
		}

		if(!noline) {			
			try {
				str = preferences.getString("trackid", "");
			} catch (Exception e33) {
				Log.e(MRDefaults.LOGTAG, "Exception: " + e33.getMessage());
				e33.printStackTrace();
				str = "";
			}		
		} else {
			str = "NOLINE";
		}

		if (us.length() == 0) {
			if (isManualParam) { 

			} else {
				Log.e(MRDefaults.LOGTAG, "Please set username");
				// Здесь надо делать нотификацию
			}
			return;
		}

		if (pw.length() == 0) {
			if (isManualParam) {

			} else { 
				Log.e(MRDefaults.LOGTAG, "Please set password");
			// Здесь надо делать нотификацию
			}
			return;
		}			

		if (isManualParam) {			 			
			if(snm.length()>0) {
				Log.d(MRDefaults.LOGTAG, "Was entered a pont name: " + snm);
			}						
			
			if(sde.length()>0) {
				Log.d(MRDefaults.LOGTAG, "Was entered a description: " + sde);
			} 			
		} 
		
		sla = this.latString;
		slo = this.lonString;		
		//sal = this.altString;
		
		CoordPoint cp = new CoordPoint(us, pw, sla, slo, sal, sdt, str, snm, sde);		
	
		Log.d(MRDefaults.LOGTAG, "us = " + cp.getUs());
		Log.d(MRDefaults.LOGTAG, "la = " + cp.getLa());
		Log.d(MRDefaults.LOGTAG, "lo = " + cp.getLo());
		Log.d(MRDefaults.LOGTAG, "al = " + cp.getAl());		
		Log.d(MRDefaults.LOGTAG, "tr = " + cp.getTr());
		Log.d(MRDefaults.LOGTAG, "dt = " + cp.getDt().replace(' ', 'T')+(cp.getDt().indexOf("Z")>0?"":"Z"));
		Log.d(MRDefaults.LOGTAG, "nm = " + cp.getNm());
		Log.d(MRDefaults.LOGTAG, "de = " + cp.getDe());							

		if (cp.getIsOk()) {			

      		if(queue.size() >= MRDefaults.MAX_QUEUE) {
    			queue.poll(); // очищаем место! 
    		}			
      		
			if( queue.size() < MRDefaults.MAX_QUEUE) {
				try {
					if(bISAautosend) { 
						if(!queue.contains(cp)) {
							queue.put(cp);
																		
							Log.v(MRDefaults.LOGTAG, "Point " + cp.getSMSBody() + " added to sending queue");
						} else {
							Log.w(MRDefaults.LOGTAG, "Point " + cp.getKey() + " already exist in queue!");							
						}
					}
					if(isManualParam) {
						queue.put(cp);
																	
						Log.v(MRDefaults.LOGTAG, "Manual point " + cp.getSMSBody() + " added to sending queue");					
					}
					if(!pList.contains(cp)) {
						pList.put(cp);						
					} else {
						Log.w(MRDefaults.LOGTAG, "Point " + cp.getKey() + " already exist in jornal!");						
					}															
				} catch (InterruptedException ex) {
					Log.e(MRDefaults.LOGTAG, "InterruptedException: " + ex.getMessage());
					ex.printStackTrace();
				}
			}											
		}		
	}

	private String MakeCoordsText() {
		String text = "";
	
		this.latString = String.format(Locale.US, "%.6f", latitude);
		this.lonString = String.format(Locale.US, "%.6f", longitude);
		//this.altString = String.format(Locale.US, "%.0f", altitude);		
	
		Log.d(MRDefaults.LOGTAG, "latString = " + latString);
		Log.d(MRDefaults.LOGTAG, "lonString = " + lonString);
		Log.d(MRDefaults.LOGTAG, "altString = " + altString);		
	
		text = "LA " + (latitude > 0 ? "N" : "") +
			   latString.replaceAll("-", "S") + " LO " +
			   (longitude > 0 ? "E" : "") + lonString.replaceAll("-", "W");
												
		return text;
	}
	
	private int GPSStatus() {
		int res  = 0;

		statusOfGPS     = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);	
		statusOfNetwork = mlocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		//statusOfPassive = mlocManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
		
		Log.d(MRDefaults.LOGTAG, "statusOfGPS=" + statusOfGPS + ", statusOfNetwork=" + statusOfNetwork + 
				"statusOfPassive = " + statusOfPassive);
		
		if(statusOfGPS)     res = res | MRDefaults.GPS_PROVIDER_OK;
		if(statusOfNetwork) res = res | MRDefaults.NETWORK_PROVIDER_OK;
//		if(statusOfPassive) res = res | MRDefaults.PASSIVE_PROVIDER_OK;
		return res;
	}
	
	private void GetGPSStatus(boolean alsoSend, boolean manualMode, 
			String snm, String sde, boolean noline, boolean cwg) {
		
		String text = "";
		int statusMask = GPSStatus(); 
		Log.d(MRDefaults.LOGTAG, "statusMask = " + statusMask);
		
		if (statusMask > 0) {
			if (latitude == 0.0 && longitude == 0.0) {
				// text = "GPS is ON";
				if(GetCoords(statusMask, cwg)) {										
					text = MakeCoordsText();	
					Log.d(MRDefaults.LOGTAG, "text = " + text);
					if(alsoSend) {
						sendCoords(manualMode, snm, sde, noline);
					}						
				} else {				
					text = "<"+getString(R.string.empty)+">";
				}
			} else {
				text = MakeCoordsText();
				if(alsoSend) {
					sendCoords(manualMode, snm, sde, noline);
				}									
			}
		} else {
			text = "" + getString(R.string.mgpsisoff); //"GPS is OFF";
		}

		sDetectedCoords = text;	
	}
	
	public boolean GetCoords(int statusMask, boolean coordsWasGet) {								
	
//		Log.d(MRDefaults.LOGTAG, "usl1 = " + (statusMask & MRDefaults.PASSIVE_PROVIDER_OK));  
		Log.d(MRDefaults.LOGTAG, "usl2 = " + (statusMask & MRDefaults.NETWORK_PROVIDER_OK));
		Log.d(MRDefaults.LOGTAG, "usl3 = " + (statusMask & MRDefaults.GPS_PROVIDER_OK));
		
		/*
		if((statusMask & MRDefaults.PASSIVE_PROVIDER_OK) == MRDefaults.PASSIVE_PROVIDER_OK) {			
			Location temploc = mlocManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			Log.d(MRDefaults.LOGTAG, "loc1 = " + temploc);
			if(temploc!=null)
				loc = temploc;						
		}					
		*/
		//mlocManager.addGpsStatusListener(listener);		
		
		if(!coordsWasGet) { 
			if(!statusOfGPS) {
				if(!statusOfNetwork) {
					return false;
				}			
				if((statusMask & MRDefaults.NETWORK_PROVIDER_OK) == MRDefaults.NETWORK_PROVIDER_OK) {
					Location temploc = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					Log.d(MRDefaults.LOGTAG, "loc2 = " + temploc);
					if(temploc!=null) {
						loc = temploc;
						prevLoc = loc;
						lPrevMillis = SystemClock.elapsedRealtime();						
					}
				}				
			} else {
				if((statusMask & MRDefaults.GPS_PROVIDER_OK) == MRDefaults.GPS_PROVIDER_OK) {			
					Location temploc = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					Log.d(MRDefaults.LOGTAG, "loc3 = " + temploc);
					if(temploc!=null) {
						loc = temploc;
						prevLoc = loc;
						lPrevMillis = SystemClock.elapsedRealtime();						
					}
				}
			}
		}

		if (loc != null) {
			Log.d(MRDefaults.LOGTAG, "loc = " + loc);
			sDetectedCoordsDateTime = MRDefaults.GenMyDate(MRDefaults.MY_DATE_FORMAT);			
			this.latitude  = loc.getLatitude();
			this.longitude = loc.getLongitude();
			this.altitude  = loc.getAltitude();			
			
			return true;
		} else {
			if (latitude != 0.0 && longitude != 0.0) {
				return true;
			}
		}
		return false;
	}	
	
	private String GetUser() {
  		try {
			us = preferences.getString("username", "").trim();
			Log.d(MRDefaults.LOGTAG, "username = " + us);
		} catch (Exception e11) {
			e11.printStackTrace();
			us = "";
		}
		return us;
	}

	private String GetPwd() {  		
		try {
			pw = preferences.getString("password", "");
		} catch (Exception e22) {
			Log.e(MRDefaults.LOGTAG, "Exception: " + e22.getMessage());
			e22.printStackTrace();
			pw = "";
		}	
		return pw;
	}
	
	private String GetTrack() {
  		try {
			tr = preferences.getString("trackid", "").trim();
		} catch (Exception e11) {
			e11.printStackTrace();
			tr = "";
		}
		return tr;
	}
	
	private void SendMessage(int msg, int arg) {

	    for (int i = mClients.size()-1; i>=0; i--) {
	        try {
	        	
		Message newMsg = Message.obtain(null, MRDefaults.MSG_SET_VALUE, msg, arg);
		
		Bundle bundle = new Bundle();
	    bundle.putString(MRDefaults.SERVSTR_RESULT,    sretvalue);
	    bundle.putString(MRDefaults.SERVSTR_COORDS,    sDetectedCoords);
	    bundle.putString(MRDefaults.SERVSTR_DATETIME,  sDetectedCoordsDateTime);
	    String str = ""+getString(R.string.queue_points)+": " + queue.size() + ", "+getString(R.string.sent)+": "+ 
		countAllOK;
	    bundle.putString(MRDefaults.SERVSTR_QUEUESIZE,  str);
	    bundle.putString(MRDefaults.SERVSTR_NETSTATUS, sNetStatus);
	    newMsg.setData(bundle);	
	    
		Log.d(MRDefaults.LOGTAG, "mClients.size() = " + mClients.size() + "[SendMessage] " + msg + ", " + arg);	    
					        	
	            mClients.get(i).send(newMsg);

	        } catch (RemoteException e) {
	            // The client is dead.  Remove it from the list;
	            // we are going through the list from back to front
	            // so this is safe to do inside the loop.
	            mClients.remove(i);
	        } catch (android.util.AndroidRuntimeException arte) {
	        	Log.e(MRDefaults.LOGTAG, "" + arte.getMessage());
	        }
	    }
	}
	
	private void loadSettings(int from) {
		String smethod = preferences.getString("method", MRDefaults.DEFAULT_METHOD);
		Log.d(MRDefaults.LOGTAG, getClass().getName() + " sendingMethod loaded = " + smethod);
		
		try {
			sendingMethod = smethod;
			if("auto".equals(sendingMethod)) {
				sendingMethod = MRDefaults.DEFAULT_METHOD; // начинаем всегда с http!
			}
			optSendingMethod = smethod;		
					
		} catch (Exception e) {
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();
	
			sendingMethod = MRDefaults.DEFAULT_METHOD;
			optSendingMethod = MRDefaults.DEFAULT_METHOD;
		}

		String sphone = preferences.getString("phone", MRDefaults.DEFAULT_PHONE);
		Log.d(MRDefaults.LOGTAG, getClass().getName() + " phone number loaded = " + sphone);
		
		try {
			phoneNo = "+" + sphone; 					
		} catch (Exception e) {
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();
			phoneNo = MRDefaults.DEFAULT_PHONE;
		}
			
		try {
			extStorageDirectory = preferences.getString("extpath", MRDefaults.DEFAULT_EXTPATH);
			Log.d(MRDefaults.LOGTAG, getClass().getName() + " extpath loaded = " + extStorageDirectory);
 					
		} catch (Exception e) {
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();
			extStorageDirectory = MRDefaults.DEFAULT_EXTPATH;
		} finally {
			if(extStorageDirectory.length()>0) isUseExtStorageDirectory = true;
			else isUseExtStorageDirectory = false;
		}
					
		String secs = preferences.getString("deltasecs", "" + MRDefaults.DEFAUIT_SECS).trim();
		Log.d(MRDefaults.LOGTAG, getClass().getName() + " deltasecs = " + secs);
	
		try {
			iSecs = Integer.parseInt(secs);
	
		} catch (Exception e) {
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();
	
			iSecs = MRDefaults.DEFAUIT_SECS;
		}
		
		String smetres = preferences.getString("deltametres", "" + MRDefaults.DEFAUIT_METRES).trim();
		Log.d(MRDefaults.LOGTAG, getClass().getName() + " deltametres = " + smetres);
	
		try {
			iMetres = Integer.parseInt(smetres);
	
		} catch (Exception e) {
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();
	
			iMetres = MRDefaults.DEFAUIT_METRES;
		}		
	}	    
	
	private void getStorageDirectory() {
		boolean isOK = false;		
		if(isUseExtStorageDirectory) {
					
	        File dir = null;
	        File file = null;			

			dir = new File(extStorageDirectory + "/" + MRDefaults.MYFOLDER + "/");
			if(!dir.exists()) {
				
				Log.v(MRDefaults.LOGTAG, "Not exists folder " + extStorageDirectory + "/" + 
						MRDefaults.MYFOLDER + "/");
				
				if(!dir.mkdirs()) {
					Log.e(MRDefaults.LOGTAG, "Could not create folder " + extStorageDirectory + "/" + 
					MRDefaults.MYFOLDER + "/");
					isOK = false;
					isUseExtStorageDirectory = false;
				} else {
					isOK = true;
					isUseExtStorageDirectory = true;
				}
			} else {
				isOK = true;
				isUseExtStorageDirectory = true;
			}
				
			if(isOK) {
				try {	
					file = new File(extStorageDirectory + "/" + MRDefaults.MYFOLDER + "/" + "test.txt");
					
			        FileWriter fwriter = new FileWriter(file);
			        BufferedWriter out = new BufferedWriter(fwriter);
	
			        out.write("test");
			        out.flush();
			        out.close();	
			        
			        file.delete();
			        isUseExtStorageDirectory = true;
	
				} catch (FileNotFoundException e) {
				    Log.e(MRDefaults.LOGTAG, "Could not found file " + "test.txt" + " for writing:" + e.getMessage());
				    isOK = false;    	
				    isUseExtStorageDirectory = false;
				} catch (IOException e) {
				    Log.e(MRDefaults.LOGTAG, "Could not write file " + "test.txt" + ":" + e.getMessage());
				    isOK = false;
				    isUseExtStorageDirectory = false;
				}
			}
		} 
		if(isOK) {
			storageDirectory = extStorageDirectory;
		}
		
		if(!isOK || !isUseExtStorageDirectory) {
	        String state = Environment.getExternalStorageState();
	        if (Environment.MEDIA_MOUNTED.equals(state)) {        
	        	storageDirectory = Environment.getExternalStorageDirectory().getPath();
	        } else {
	        	storageDirectory = Environment.getDataDirectory().getPath();
	        	Log.e(MRDefaults.LOGTAG, "Environment.MEDIA_MOUNTED state = " + state);                	
	        }
	        Log.d(MRDefaults.LOGTAG, "storageDirectory = " + storageDirectory);			
		}       		
	}
	
	private boolean SaveQueue() {
		boolean isOK = false;				
                        
        File dir = null;
        File file = null;

		try {
			dir = new File(storageDirectory + "/" + MRDefaults.MYFOLDER + "/");
			if(!dir.exists()) {
				if(!dir.mkdirs()) {
					Log.e(MRDefaults.LOGTAG, "Can not create folder " + storageDirectory + "/" + 
					MRDefaults.MYFOLDER + "/");
					isOK = false;
				}
			}
			file = new File(storageDirectory + "/" + MRDefaults.MYFOLDER + "/" + queueFileName);
			
			if(queue.isEmpty()) {  
				if(file.exists()) {
   					if(file.delete()) { // очередь пуста, сохранять нечего, удаляем!
   						Log.v(MRDefaults.LOGTAG, "Queue file was deleted because queue is empty");
   					} else {
   						Log.v(MRDefaults.LOGTAG, "Queue file can not deleted but queue is empty");
   					}
				}
				isOK = true; // притворяемся, что всегда все хорошо!
			} else {   				
			
    	        FileWriter gwriter = new FileWriter(file);
    	        BufferedWriter out = new BufferedWriter(gwriter);
    	        
    	        // тут надо сохранять очередь!
    	        //out.write("Hello world");
    	        //String sqb = writeQueueXmlAsGPX();
    	        MRJournal mrj = new MRJournal(GetUser(), GetTrack(), queue); 
    	        String sqb = mrj.writeQueueXml();
    	        Log.d(MRDefaults.LOGTAG, "xml = " + sqb);
    	        out.write(sqb);
    	        out.flush();
    	        out.close();	    	        	    	        
    
    	        isOK = true;
    	        
    		    //outStream = new FileOutputStream(file);
    		    //bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
    		    //outStream.flush();
    		    //outStream.close();
			}
		} catch (FileNotFoundException e) {
		    Log.e(MRDefaults.LOGTAG, "Could not find file " + queueFileName + ":" + e.getMessage());
		    isOK = false;    			
		} catch (IOException e) {
		    Log.e(MRDefaults.LOGTAG, "Could not write file " + queueFileName + ":" + e.getMessage());
		    isOK = false;
		} finally {

		}
        
        if(isOK) {
        	if(!queue.isEmpty()) {
        		Log.v(MRDefaults.LOGTAG, "File " + storageDirectory + "/" + MRDefaults.MYFOLDER + "/"+ 
        		queueFileName + " saved! ");
        	}
        	
			SharedPreferences.Editor editor = preferences.edit();			
			editor.putString("lastPath", storageDirectory);
			editor.commit();
        }
        
		return isOK;
	}		

	private boolean LoadQueueXML(InputStream in) {
		boolean isOK = true;
		
		Log.v(MRDefaults.LOGTAG, "Loading queue...");
		
		if(!queue.isEmpty()) {
			Log.d(MRDefaults.LOGTAG, "LoadQueue: queue already not empty");
			//return false; // очередь уже не пуста!
		}
		
		XmlPullParser parser = Xml.newPullParser();
//		List<Message> messages = null;
		
  		String us = "";
		String pw = "";
		String la = "";
		String lo = "";
		String al = "";
  	  	String dt = "";
	  	String tr = "";
	  	String nm = "";
	  	String de = "";		
	  	
	  	us = GetUser();
	  	pw = GetPwd();  
	
		CoordPoint cp = null;
		boolean newPoint = false;
		try {
	          parser.setInput(in, "UTF-8"); // null
              int eventType = parser.getEventType();
              
              boolean done = false;
              while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                  String name = null;
                  switch (eventType) {
                      case XmlPullParser.START_DOCUMENT:
                          //messages = new ArrayList<Message>();
                          break;
                      case XmlPullParser.START_TAG:
                          name = parser.getName();
                          if (name.equalsIgnoreCase("point")) {
                              newPoint = true;
                              
                        	  String templa = parser.getAttributeValue("", "la");
                        	  String templo = parser.getAttributeValue("", "lo");
                        	  String tempal = parser.getAttributeValue("", "al");
                        	  String tempdt = parser.getAttributeValue("", "dt");
                        	  String temptr = parser.getAttributeValue("", "tr");
                        	  String tempnm = parser.getAttributeValue("", "nm");
                        	  
                        	  if(templa!=null)
                        		  if(templa.length()>0) la = templa;
                        	  if(templo!=null)
                        		  if(templo.length()>0) lo = templo;
                        	  if(tempal!=null)
                        		  if(tempal.length()>0) al = tempal;
                        	  if(tempdt!=null)
                        		  if(tempdt.length()>0) dt = tempdt;
                        	  if(temptr!=null)
                        		  if(temptr.length()>0) tr = temptr;
                        	  if(tempnm!=null)
                        		  if(tempnm.length()>0) nm = tempnm;                        	                          	                         	                          	  
                        	  
                        	  String tempDe = parser.getText();
                        	  if(tempDe!=null) {
                        		  if(tempDe.length()>0) {
                        		  	  de = tempDe; 
                        		  }
                        	  }
                        	  
                        	  cp = new CoordPoint(us, pw, la, lo, al, dt, tr, nm, de);
                        	                          	  
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "us = " + cp.getUs());
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "la = " + cp.getLa());
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "lo = " + cp.getLo());
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "al = " + cp.getAl());
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "dt = " + cp.getDt());
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "tr = " + cp.getTr());
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "nm = " + cp.getNm());
	                    	  Log.d(MRDefaults.LOGTAG, "Loaded: " + "de = " + cp.getDe());                        	                                
                              
                          } else if (newPoint) {
                        	                          	  
                        	  newPoint = false;
                          } else {
                        	                   	                          	  	                    		
                        	  /*
                              if (name.equalsIgnoreCase(LINK)){
                                  currentMessage.setLink(parser.nextText());
                              } else if (name.equalsIgnoreCase(DESCRIPTION)){
                                  currentMessage.setDescription(parser.nextText());
                              } else if (name.equalsIgnoreCase(PUB_DATE)){
                                  currentMessage.setDate(parser.nextText());
                              } else if (name.equalsIgnoreCase(TITLE)){
                                  currentMessage.setTitle(parser.nextText());
                              }   
                              */
                          }
                          break;
                      case XmlPullParser.END_TAG:
                          name = parser.getName();
                          Log.d(MRDefaults.LOGTAG, "End tag: " +name);
                          if (name.equalsIgnoreCase("point") && cp != null){
                        	  
		                  		if (cp.getIsOk() && queue.size() < MRDefaults.MAX_QUEUE) {
		                			try {
		                				if(!queue.contains(cp))
		                					queue.put(cp);
		                				else Log.w(MRDefaults.LOGTAG, "Queue: " + cp.getKey()+" already exist!");
		                				if(!pList.contains(cp))		                				
		                					pList.put(cp);
		                				else Log.w(MRDefaults.LOGTAG, "Journal: " + cp.getKey()+" already exist!");
		                				
		                				Log.d(MRDefaults.LOGTAG, "Queue loaded: " + queue.size());
		
		                			} catch (InterruptedException ex) {
		                				Log.e(MRDefaults.LOGTAG, "InterruptedException: " + ex.getMessage());
		                				ex.printStackTrace();
		                			}
		                		}                        	                         	                          	  
                          } else if (name.equalsIgnoreCase("myroad")){
                              done = true;
                          }
                          break;
                  }
                  eventType = parser.next();    
              }		                				
              //tView4.setText(tV4Prefix + ": " + queue.size() + " points in queue, sent: " + countAllOk);
			
		}  catch (Exception e) {
//            throw new RuntimeException(e);
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " XML Parser exception: " + e.getMessage());
			e.printStackTrace();	
			isOK = false;
        }		
				
		return isOK;
	}
	
	private boolean LoadQueue() {
		boolean isOK = false;
		String spath = preferences.getString("lastPath", "");
		if(spath.length()>0) {
			try {
				File myFile = new File(spath + "/" + MRDefaults.MYFOLDER + "/" + queueFileName);
				FileInputStream fin = new FileInputStream(myFile);
				
				if(LoadQueueXML(fin)) isOK = true;
				
				fin.close();			
				if(myFile.delete()) {
					Log.e(MRDefaults.LOGTAG, "Could not delete file " + queueFileName + " after loading");
				}
    		} catch (FileNotFoundException e) {
    		    Log.e(MRDefaults.LOGTAG, "Could not found file " + queueFileName + " for loading: " + e.getMessage());
    		    isOK = false;   			
    		} catch (IOException e) {
    		    Log.e(MRDefaults.LOGTAG, "Could not read file " + queueFileName + ": " + e.getMessage());
    		    isOK = false;
    		}
		} else {
			return true;
		}
		return isOK;
	}
	
	private boolean SaveJournal(StringWriter writer, String saveFileName)
	{
		boolean isOK = false;
				
        File dir = null;
        File file = null;
        
        //getStorageDirectory();
        
		try {
			dir = new File(storageDirectory + "/" + MRDefaults.MYFOLDER + "/");
			if(!dir.exists()) {
				if(!dir.mkdirs()) {
					Log.e(MRDefaults.LOGTAG, "Can not create folder " + storageDirectory + "/" + MRDefaults.MYFOLDER + "/");
					isOK = false;
				}
			}
			file = new File(storageDirectory + "/" + MRDefaults.MYFOLDER + "/" + saveFileName);
			
	        FileWriter gwriter = new FileWriter(file);
	        BufferedWriter out = new BufferedWriter(gwriter);
	        
	        String sxml = writer.toString();
	        Log.d(MRDefaults.LOGTAG, "xml = " + sxml);
	        out.write(sxml);
	        out.flush();
	        out.close();

	        isOK = true;
		} catch (FileNotFoundException e) {
		    Log.e(MRDefaults.LOGTAG, "Could not found file " + saveFileName + " for writing:" + 
		    		e.getMessage());
		    isOK = false;    			
		} catch (IOException e) {
		    Log.e(MRDefaults.LOGTAG, "Could not write file " + saveFileName + ":" + e.getMessage());
		    isOK = false;
		}					
		return isOK;
	}	
	
	private void SaveKml() {
	 	MRKml lkml = new MRKml(GetTrack(), pList);
    	lkml.StartKML();		
		SaveJournal(lkml.EndKML(), kmlFileName);   
	}
	
	private void SaveGpx() {
	 	MRGpx lgpx = new MRGpx(GetTrack(), pList);
    	lgpx.StartGPX();		
		SaveJournal(lgpx.EndGPX(), gpxFileName);   
	}
	
}