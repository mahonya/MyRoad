package myroad.myroadpack.namespace;

import android.app.Application;


public class MyRoadApplication extends Application {

	MyRoadService navigationService;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();		
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();		
	}	
	
	public MyRoadService getNavigationService() {
		return navigationService;
	}

	public void setNavigationService(MyRoadService navigationService) {
		this.navigationService = navigationService;
	}	
}