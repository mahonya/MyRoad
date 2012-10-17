package myroad.myroadpack.namespace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class MRAlarmReceiver extends BroadcastReceiver {
		@Override
	public void onReceive(Context context, Intent intent) {
		final WakeLock lock = MyRoadService.getLock(context);
		final MyRoadService service = ((MyRoadApplication) context.getApplicationContext()).getNavigationService();
		// do not do nothing
		if (lock.isHeld() || service == null) {
			//Log.d(MRDefaults.LOGTAG, getClass().getName() + " lock.isHeld(): " + lock.isHeld() + 
			//		", service: " + (service==null?"null":"not null"));
			return;
		}
		//
		lock.acquire();
		// request location updates
		final LocationManager locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);
		try {
			Log.d(MRDefaults.LOGTAG, getClass().getName() + ".requestLocationUpdates");			
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, service);
		} catch (RuntimeException e) {
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	}