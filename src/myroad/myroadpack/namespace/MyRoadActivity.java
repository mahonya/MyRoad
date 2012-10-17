package myroad.myroadpack.namespace;

import java.lang.ref.WeakReference;
import java.util.Locale;

import android.widget.Toast;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class MyRoadActivity extends Activity implements OnClickListener {

	final static String LOGTAG = "my-road";
	
	final static String appTitle = MRDefaults.appTitle;
	
    private Locale locale;
    private String lang;	
	
	//private static ProgressDialog progress = null;
	
	Button mButton5;
	TextView tView1; // Coords
	TextView tView2; // Detected
	//TextView tView3; // Mode
	TextView tView4; // Track
	TextView tView5; // Network
			
	EditText eText1; // Point name
	EditText eText2; // Point description
	
	CheckBox chBox1;

	//String locationText = "";
	
	String tV1Prefix = ""; // Prefix for Coords
	String tV2Prefix = ""; // Prefix for Detected	
	String tV3Prefix = ""; // Prefix for Mode
	String tV4Prefix = ""; // Prefix for Track	
	String tV5Prefix = ""; // Prefix for Network	
	
	String text1Default = "";
	
    private boolean isIamVisible = false;
    
    private int nManualPoints = 0;

	private SharedPreferences preferences;
	OnSharedPreferenceChangeListener prefsListener;
	
	Messenger mService = null;
	
	boolean mIsBound = false;
	
    static class IncomingHandler extends Handler {    
    	private final WeakReference<MyRoadActivity> mra;
    	
    	IncomingHandler(MyRoadActivity activity) {
    		mra = new WeakReference<MyRoadActivity>(activity);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
        	MyRoadActivity acti = mra.get();
        	if(acti != null) {
        		acti.handleMessage(msg);
        	}
        }        	
    }    		
    IncomingHandler inh = new IncomingHandler(this);    
    final Messenger mMessenger = new Messenger(inh);        
	
//	final Messenger mMessenger = new Messenger(new IncomingHandler());	
	
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
	        //tView1.setText("Attached.");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null, MRDefaults.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mService.send(msg);

	            // Give it some value as an example.
	            msg = Message.obtain(null, MRDefaults.MSG_SET_VALUE, this.hashCode(), 0);
	            mService.send(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        	Log.e(LOGTAG, e.getMessage());
	        }

	        // As part of the sample, tell the user what happened.
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        //tView1.setText("Disconnected.");


	    }
	};		
          
	
	public void handleMessage(Message msg) {
        switch (msg.what) {
            case MRDefaults.MSG_SET_VALUE:
            	
/*
    bundle.putString(MRDefaults.SERVSTR_RESULT,    sretvalue);
    bundle.putString(MRDefaults.SERVSTR_COORDS,    sDetectedCoords);
    bundle.putString(MRDefaults.SERVSTR_DATETIME,  sDetectedCoordsDateTime);	    	    	    
    bundle.putString(MRDefaults.SERVSTR_QUEUESIZE, "" + queue.size());
    bundle.putString(MRDefaults.SERVSTR_NETSTATUS, sNetStatus); 
	                
*/
                if(msg.arg1 == MRDefaults.SERVMSG_QUEUE_WAS_SENT) {
                	Bundle data = msg.getData();
                	String srestext = data.getString(MRDefaults.SERVSTR_RESULT);
                	if(srestext.length()>0 && msg.arg2>0) {
                		if(isIamVisible)
                			tView5.setText(srestext);
                	} else {
                		String s4 = data.getString(MRDefaults.SERVSTR_QUEUESIZE);
                		if(isIamVisible && s4.length()>0) {
                			tView4.setText(tV4Prefix + ": " + s4);
                		}
                	}
                	
                	Log.d(MRDefaults.LOGTAG, "srestext=" + srestext);
                }
                
                if(msg.arg1 == MRDefaults.SERVMSG_LAST_STATE || 
                		msg.arg1 == MRDefaults.SERVMSG_TIME_TICK) {
                	Bundle data = msg.getData();
                	String s1 = data.getString(MRDefaults.SERVSTR_COORDS);
                	String s2 = data.getString(MRDefaults.SERVSTR_DATETIME);	                	
                	String s4 = data.getString(MRDefaults.SERVSTR_QUEUESIZE);	                	
                	String s5 = data.getString(MRDefaults.SERVSTR_NETSTATUS);
                	if(isIamVisible) {
                		if(s1.length()>0)
                			tView1.setText(tV1Prefix + ": " + s1);
                		if(s2.length()>0)
                			tView2.setText(tV2Prefix + " " + s2);
                		if(s4.length()>0)
                			tView4.setText(tV4Prefix + ": " + s4);
                		if(s5.length()>0)
                			tView5.setText(tV5Prefix + ": " + s5);
                	}
                }
            	
                break;
            default:
                //super.handleMessage(msg);
            	break;
        }
    }	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
								
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		lang = preferences.getString("lang", "default");	
        if (lang.equals("default")) {
        	lang = getResources().getConfiguration().locale.getCountry();
        }
        locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, null);		
		
		mButton5 = (Button) findViewById(R.id.button5); // AddPoint
		mButton5.setOnClickListener(this);		

		tView1 = (TextView) findViewById(R.id.textView1); // Coords
		tView2 = (TextView) findViewById(R.id.textView2); // Detected
//		tView3 = (TextView) findViewById(R.id.textView3); // Mode		
		tView4 = (TextView) findViewById(R.id.textView4); // Points		
		tView5 = (TextView) findViewById(R.id.textView5); // Network
		
		tV1Prefix = tView1.getText().toString();
		tV2Prefix = tView2.getText().toString();
		tV4Prefix = tView4.getText().toString();		
		tV5Prefix = tView5.getText().toString();	

		tView1.setText("<"+getString(R.string.empty)+">");

		eText1 = (EditText) findViewById(R.id.editText1);
		eText1.setFocusableInTouchMode(true);
		eText1.selectAll();
		text1Default = eText1.getText().toString().trim();
		
		eText2 = (EditText) findViewById(R.id.editText2);
		eText2.setFocusableInTouchMode(true);
		eText2.selectAll();
		
		chBox1 = (CheckBox) findViewById(R.id.checkBox1);
			
		// queue.size will get from service!
		//tView4.setText(tV4Prefix + ": " + queue.size() + " points in queue, sent: " + countAllOK);		
			
		//progress = new ProgressDialog(this);
		//progress.setMessage("Loading...");
		
//		if(LoadQueue()) {
			// queue.size will get from service!			
//			Log.v(LOGTAG, "Sending queue loaded OK");
//			if(queue.size()>0)
//			tView4.setText(tV4Prefix + ": " + queue.size() + " points in queue "+(queue.size()>0?"loaded":""));
		
//		}	
						    		
		//registerReceiver(receiver, mNetworkStateChangedFilter);
						
		if(!CheckUsPw()) {	
			Intent i = new Intent(this, PreferencesActivity.class);
			startActivity(i);			
		}
		
		if(CheckUsPw()) {
			Intent service = new Intent(this, MyRoadService.class);
    		startService(service);
		
    		doBindService();
		}		
	}			

	public boolean CheckUsPw() {
		String us = "";
		String pw = "";
		
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
		Context context = getApplicationContext();
		boolean needEditOptions = false;
		
		if (us.length() == 0) {
			needEditOptions = true;
			Toast.makeText(context, ""+getString(R.string.please_su), Toast.LENGTH_SHORT).show();
			return false;
		}
		if (pw.length() == 0) {
			needEditOptions = true;
			Toast.makeText(context, ""+getString(R.string.please_sp), Toast.LENGTH_SHORT).show();
		}
		
		return !needEditOptions;
	}
	
	@Override
	protected void onDestroy() {
//		if(SaveQueue()) { 		
			SharedPreferences.Editor editor = preferences.edit();
			
//			if(storageDirectory.length()>0) {
//				editor.putString("lastPath", storageDirectory);
//			}
			editor.commit();
//		}		
			
		Log.v(MRDefaults.LOGTAG, "Activity.onDestroy()");
		
		// надо передавать в сервис сообщение о необходимости сохранения координат 		
		doUnbindService();				
		
	    //Intent service = new Intent(this, MyRoadService.class);
	    //stopService(service);
	    
		super.onDestroy();	    
	}
	
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, null);
        
        /*
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        */        
    }		
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// надо посылать событие в сервис что можно сохранять координаты 
		Log.v(LOGTAG, "Activity Pause...");
				
		isIamVisible = false;
	}	

	@Override
	protected void onResume() {
		super.onResume();
		
		doBindService();
		
		Log.v(LOGTAG, "Activity Resume...");
				
		isIamVisible = true;
	}			
	
	protected void onRestart() {
		super.onRestart();						
		
		// надо сигнализировать сервису что пора перечитать настройки
		Log.v(LOGTAG, "Activity Restart...");	
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.icon_menu, menu);
        return true;
    }
	
	public void onClick(View v) {
				
		switch (v.getId()) {		
		case R.id.button5: // AddPoint
			// нужно отправить сообщение в сервис: поставь точку с текущими координатами
			
			String lastNMValue  = eText1.getText().toString().trim();		
			String lastDEValue = eText2.getText().toString();
			boolean noline = chBox1.isChecked();
			
			if(text1Default.equals(lastNMValue)) {
				nManualPoints++;
				lastNMValue = lastNMValue + nManualPoints;
			}
			
			sendMessage2ServiceExt(MRDefaults.MSG_NAME_LAST_STATE,
					lastNMValue,
					lastDEValue,
					noline);
		
			break;			
		}
		
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {        

        switch (item.getItemId()) {
/*        
		case R.id.send: // Send coords - надо запускать Async task вручную?			
			return true;
		case R.id.view: // получить координаты
			sendMessage2Service(MRDefaults.MSG_GET_LAST_STATE);
			return true;
*/			
        
        case R.id.options:
        	// отключаем сервис на время работы с настройками
    	    Intent service = new Intent(this, MyRoadService.class);
    	    stopService(service);
    	    
			Intent i = new Intent(this, PreferencesActivity.class);
			startActivity(i);
			
			if(CheckUsPw()) {			
				service = new Intent(this, MyRoadService.class);
		    	startService(service);
			}

            return true;
        
        case R.id.exit:
        	// отключаем сервис перед выходом из приложения
        	// надо
        	doUnbindService();
        	
		    service = new Intent(this, MyRoadService.class);
		    stopService(service);		    
		    
        	finish();
			System.runFinalizersOnExit(true);
			System.exit(0);
        	
            return true;
            
        default:
            return super.onOptionsItemSelected(item);
        }
    }	
	
	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
		if(!mIsBound) {
			mIsBound = bindService(new Intent(this, 
		    		MyRoadService.class), mConnection, Context.BIND_AUTO_CREATE);
			
		    if(mIsBound) {
		    	//tView1.setText("Binding.");
		    } else {
		    	Log.e(MRDefaults.LOGTAG, "Could not bind a service");
		    }
		}
	    
	    // Create a new Messenger for the communication back
	    /*
	    Messenger messenger = new Messenger(handler);
	    intent.putExtra("MESSENGER", messenger);
	    intent.setData(Uri.parse("http://www.vogella.com/index.html"));
	    intent.putExtra("urlpath", "http://www.vogella.com/index.html");
	    startService(intent);
	    */
	}	
	
	void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null, MRDefaults.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            	Log.e(MRDefaults.LOGTAG, e.getMessage());
	            }
	        }

	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}	

	void sendMessage2Service(int msgCode) {
	    if (mIsBound) {
	        if (mService != null) {
	            try {
	                
	            	Message msg = Message.obtain(null, MRDefaults.MSG_SET_VALUE, msgCode, 0);	            		        	    
		            mService.send(msg);
	                
	            } catch (RemoteException e) {
	            	Log.e(LOGTAG, e.getMessage());
	            }
	        }
	    }		
	}	
	
	void sendMessage2ServiceExt(int msgCode, String nm, String de, boolean noline) {
	    if (mIsBound) {
	        if (mService != null) {
	            try {
	                
	            	Message msg = Message.obtain(null, MRDefaults.MSG_SET_VALUE, msgCode, 0);
	            	
	        		Bundle bundle = new Bundle();
	        	    bundle.putString(MRDefaults.STR_POINTNAME,    nm);
	        	    bundle.putString(MRDefaults.STR_POINTDESC,    de);
	        	    bundle.putString(MRDefaults.STR_POINTNOLINE,  (noline?"NOLINE":""));	        	    
	        	    msg.setData(bundle);		
	        	    
		            mService.send(msg);
	                
	            } catch (RemoteException e) {
	            	Log.e(LOGTAG, e.getMessage());
	            }
	        }
	    }		
	}	
		
}
