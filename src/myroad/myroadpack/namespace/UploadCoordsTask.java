package myroad.myroadpack.namespace;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;


public class UploadCoordsTask extends AsyncTask<String, Void, String> {
	
	int errno = 0;

    int countAll = 0;
    int countAllOK = 0;
	int curElem = 0;
	
	String phoneNo = MRDefaults.DEFAULT_PHONE;
	String user = "";
	String lang = "";
	
	String sendingMethod = MRDefaults.DEFAULT_METHOD;
	
	String sretvalue = "";
	    
    private SmsManager smsManager;    
	
	boolean isSenderWorkNow = false;
	boolean isAllSendedOK = false;

	Context context;
	
	private BlockingQueue<CoordPoint> queue; 	
			
	UploadCoordsTask(String newUser, String newSendingMethod, String newPhone,
			BlockingQueue<CoordPoint> newQueue, Context newContext, SmsManager newSmsManager,
			String newLang
			) {
		
		user          = newUser;
		sendingMethod = newSendingMethod;
		phoneNo       = newPhone;
		queue         = newQueue;
		context       = newContext;
		smsManager    = newSmsManager;
		lang          = newLang;
		
		//smsReceiver1 = sms1;
		//smsReceiver2 = sms2;		
		//mrservice = serv;
	}
	
	protected String doDO(String... params) {	
		return doBody(params);		
	}
	
	private String doBody(String... params) {
		String url = params[0];
		
		String sresponse = "";

		if(user.length()==0) {
			Log.e(MRDefaults.LOGTAG, "Please set user name");
			return "";
		} else {
			Log.v(MRDefaults.LOGTAG, "Trying [" + sendingMethod + "] to sending coords for user: " + user);
		}		

		HttpEntity ent = null;
		int count = 0;
		errno = 0;

		HttpClient httpclient = null;
		HttpPost httppost = null;
		
		if(sendingMethod.equals("HTTP") || sendingMethod.equals("auto")) {
			try {
				httpclient = new DefaultHttpClient();
				httppost = new HttpPost(url);
			} catch (Exception e) {
				Log.e(MRDefaults.LOGTAG, "Error: " + e.getMessage());
				e.printStackTrace();
				sretvalue = "Error: " + e.getMessage();
				this.notifyAll();
				return "";
			}
		}
		
		Log.d(MRDefaults.LOGTAG, "Upload coord task: queue size: " + queue.size());
		
		int qsize = 0;
		while (!queue.isEmpty()) {
			qsize = queue.size(); 
			
			Log.d(MRDefaults.LOGTAG, "sendingMethod = " + sendingMethod + ", queue size: " + queue.size());
			
			CoordPoint cp = (CoordPoint) queue.peek();
			if(cp!=null) {
				Log.d(MRDefaults.LOGTAG, "Coord point read from queue");
			} else {
				Log.e(MRDefaults.LOGTAG, "Coord point is null");
				errno ++;
				break;					
			}
					
			// HTTP
			if(sendingMethod.equals("HTTP") || sendingMethod.equals("auto")) { 
			ent = cp.getHttpEntity();		

			if (ent != null) {
				try {
				    httppost.setHeader("User-Agent", "MyRoad Android Tracker");
				    //httppost.setHeader("Accept-Charset", "utf-8");
				    //httppost.setHeader("Accept-Language", "en-us");		
				    //httppost.setHeader("Accept", "text/*");
				    httppost.setHeader("Content-Type", "text/html; charset=utf-8"); 
				    // тут должна быть локализация!
				    //httppost.setHeader("Content-Language", ""+lang);
				    Log.d(MRDefaults.LOGTAG, "Content-Language: " + lang);

					//httppost.setParams(params);
					httppost.setEntity(ent);
					Log.d(MRDefaults.LOGTAG, "uri: " + httppost.getURI());
					Log.d(MRDefaults.LOGTAG, "Sending point: " + cp.getBody());

					HttpResponse response = httpclient.execute(httppost);
					StatusLine statusLine = response.getStatusLine();

					Log.d(MRDefaults.LOGTAG, "statusLine: " + statusLine.toString());

					int statusCode = statusLine.getStatusCode();
					
					Log.d(MRDefaults.LOGTAG, "Site status code: "+statusCode);

					if (statusCode == 200) { // HTTP OK
						HttpEntity entity = response.getEntity();
						if (entity != null) {
							InputStream content = entity.getContent();
							
							Log.d(MRDefaults.LOGTAG, "Request content = " + content);

							BufferedReader buffer = new BufferedReader(
									new InputStreamReader(content));

							sresponse = "";
							String s = "";
							while ((s = buffer.readLine()) != null) {
								Log.d(MRDefaults.LOGTAG, s);
								sresponse += s;
							}
							Log.d(MRDefaults.LOGTAG, "Response: " + sresponse);
							//sretvalue = sresponse;

							queue.poll();
							Log.d(MRDefaults.LOGTAG, "Coord point poll from queue, new size = " + 
									queue.size());
							
							buffer.close();								
							content.close();
							entity.consumeContent();
												
							countAll++;
							count++;
						} else {
							Log.e(MRDefaults.LOGTAG, "Empty entity in response");								
						}
					} else { // HTTP error
						Log.d(MRDefaults.LOGTAG, "body: "+cp.getBody());
						Log.e(MRDefaults.LOGTAG, "HTTP error: " + HttpStatusCode2Message(statusCode));
						sretvalue = "HTTP error: " + HttpStatusCode2Message(statusCode);
						errno++;						
						break;						
					}						
				} catch (UnknownHostException e) {
					Log.e(MRDefaults.LOGTAG, "UnknownHostException: " + e.getMessage());
					e.printStackTrace();
					sretvalue = "UnknownHostException: " + e.getMessage();
					Log.d(MRDefaults.LOGTAG, "queue size = " + queue.size());
					errno++;
					break;

				} catch (Exception ex) {
					Log.e(MRDefaults.LOGTAG, "Error: " + ex.getMessage());
					ex.printStackTrace();
					sretvalue = "Error: " + ex.getMessage();
					Log.d(MRDefaults.LOGTAG, "queue size = " + queue.size());
					errno++;						
					break;
				}
			} else {
				Log.d(MRDefaults.LOGTAG, "cp.getHttpEntity() = null");
			}				
			}
			
			// SMS
			if(sendingMethod.equals("SMS")) {
				if(count>=qsize) { 
					break;				
				}										
								
				try {					
					Log.d(MRDefaults.LOGTAG, "Sending SMS number " + (curElem+1) + " to number " + phoneNo);
					
					Log.d(MRDefaults.LOGTAG, "cp = " + cp);
										
				    Intent intent1 = new Intent(MRDefaults.SENT);
				    Intent intent2 = new Intent(MRDefaults.DELIVERED);
				    intent1.putExtra("myroad.myroadpack.namespace." + MRDefaults.MR_SMS_SIGN, cp.getKey());
				    intent2.putExtra("myroad.myroadpack.namespace." + MRDefaults.MR_SMS_SIGN, cp.getKey());
				    			    				    									
				    intent1.putExtra("myroad.myroadpack.namespace." + MRDefaults.MR_COORD_POINT, cp);
				    intent2.putExtra("myroad.myroadpack.namespace." + MRDefaults.MR_COORD_POINT, cp);					
				    
				    PendingIntent sentPI = PendingIntent.getBroadcast(context, MRDefaults.BROD_SMS_SENT,
				        intent1, 0);
				 
				    PendingIntent deliveredPI = PendingIntent.getBroadcast(context, MRDefaults.BROD_SMS_DELIVER,
				        intent2, 0);				    				    				 				    
				    
				    Log.d(MRDefaults.LOGTAG, "SMS: " + cp.getBody());
					smsManager.sendTextMessage(phoneNo, null, cp.getBody(), sentPI, deliveredPI);
					queue.poll();
					
				} catch (Exception e) {
					Log.e(MRDefaults.LOGTAG, "Error: " + e.getMessage());
					e.printStackTrace();
					sretvalue = "Error: " + e.getMessage();
					Log.d(MRDefaults.LOGTAG, "queue size = " + queue.size());
					errno++;						
					break;
				}
			}																
			
			if(count > MRDefaults.MAX_COUNT || errno > MRDefaults.MAX_ERRNO) { 
				break;				
			}
			//Log.d(LOGTAG, "count queue:" + count);							
			curElem = count;
			count++;				
			Log.d(MRDefaults.LOGTAG, "count queue:" + count);

		} // while
					
		if(sendingMethod.equals("HTTP") || sendingMethod.equals("auto")) {
			httpclient.getConnectionManager().closeExpiredConnections();
			if(errno==0) countAllOK++;
		} else {
			//if(errno==0) queue.clear();
		}
		
		if(errno==0) {			
			//sretvalue = count + " points was sent";
			isAllSendedOK = true; 
		} else {
			isAllSendedOK = false;			
		}

		return "";
	}
	
	@Override
	protected String doInBackground(String... params) {
		return doBody(params);
	}

	@Override
	protected void onPreExecute() {		
		super.onPreExecute();
		
		isSenderWorkNow = true;
		/*
		if (MyRoadActivity.this.isManual) {
			if (progress.isShowing()) {
				progress.dismiss();
			}			
			
			progress = ProgressDialog.show(MyRoadActivity.this,	"Sending a coords...", 
					"Wait a moment please", true, true);
		}
		*/
	}

	@Override
	protected void onPostExecute(String result) {				
		super.onPostExecute(result);
		
		
		Log.d(MRDefaults.LOGTAG, "onPostExecute errno="+errno);
		
		isSenderWorkNow = false;		
	}	
	

	public String HttpStatusCode2Message(int code)
	{
		switch(code) {
			case 403:
				return ("Wrong username or password on " + MRDefaults.baseURL);	
		}
		return ("" + code);
	}
}