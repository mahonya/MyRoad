package myroad.myroadpack.namespace;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class MRDefaults {
	final static String LOGTAG = "my-road";	
	
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_VALUE = 3;    
    
    static final int MSG_GET_LAST_STATE = 100;    // вернуть из сервиса тек. состо€ние однократно
    static final int MSG_SUB_LAST_STATE = 101;    // клиент подписываетс€ на получение состо€ний
    static final int MSG_UNSUB_LAST_STATE = 102;  // клиент отписываетс€
    static final int MSG_NAME_LAST_STATE = 103;   // клиент именует последнюю точку
    
    static final int SERVMSG_TIME_TICK = 104;   // произошло событие таймера    
    static final int SERVMSG_QUEUE_WAS_SENT = 105;   // попытались отправить очередь
    static final int SERVMSG_LAST_STATE = 106;   // последнее состо€ние
    
    static final String STR_POINTNAME = "POINTNAME";
    static final String STR_POINTDESC = "POINTDESC";
    static final String STR_POINTNOLINE = "POINTNOLINE";    
    
	static final String SERVSTR_RESULT = "RESULT";
	static final String SERVSTR_COORDS = "COORDS";
	static final String SERVSTR_DATETIME = "DATETIME";
	static final String SERVSTR_QUEUESIZE = "QUEUESIZE";
	static final String SERVSTR_NETSTATUS = "NETSTATUS";	
    
    static final int TRUE = 1;    
    static final int GPS_PROVIDER_OK = 1;
    static final int NETWORK_PROVIDER_OK = 2;
    static final int PASSIVE_PROVIDER_OK = 4;
    
	static final int MAX_COUNT = 32;
	static final int MAX_ERRNO = 3;   
	
	static final int MAX_QUEUE = 2048;
	static final int MAX_QUEUE2 = 2048*32;
    
	static final String MR_SMS_SIGN = "my-road.sms_body";
	static final String MR_SMS_SER = "my-road.point_full";
	static final String MR_COORD_POINT = "CoordPoint";
	
	static final String SIMPLE_DATE_FORMAT = "yy-MM-dd HH:mm";	
	static final String MY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	static final String DEFAULT_METHOD = "HTTP";
	static final String DEFAULT_PHONE = "79237377623";
	
	static final int BROD_SMS_SENT = 1111;
	static final int BROD_SMS_DELIVER = 2222;
	
	static final int DEFAUIT_SECS = 120;	
	static final int DEFAUIT_METRES = 1000;
	
	static final String DEFAULT_EXTPATH = "/mnt/extsd";	
	
	static final String baseURL = "http://my-road.info";
	static final String appTitle = "MyRoad Tracker";
	final static String MYFOLDER = "my-road";
	
	static final String SENT = "myroad.SMS_SENT";
	static final String DELIVERED = "myroad.SMS_DELIVERED";
	
	static final boolean isUTC = true;
	
	public static String GenMyDate(String format) { 
		Calendar c = Calendar.getInstance();
		if(isUTC) {
			TimeZone gmtTime = TimeZone.getTimeZone("GMT");		
			SimpleDateFormat df = new SimpleDateFormat(format);		
			df.setTimeZone(gmtTime);
			String formattedDate = df.format(c.getTime());
			return formattedDate;			
		} else {
			SimpleDateFormat df = new SimpleDateFormat(format);
			String formattedDate = df.format(c.getTime());
			return formattedDate;			
		}					
	}	
		
	/*
	Calendar c = Calendar.getInstance();
	SimpleDateFormat df = new SimpleDateFormat(MY_FILEDATE_FORMAT);
	String formattedDate = df.format(c.getTime());
	*/
	
	public static String getQueueFileName(String user, String track, String ext) {
		return "myroad_" + user+"_" + track + "." + ext;
	}
	
	    
}