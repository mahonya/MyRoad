package myroad.myroadpack.namespace;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class MRJournal {
	
	private String user  = "";
	private String track = "";
	
	private BlockingQueue<CoordPoint> queue;
	
	MRJournal(String newUser, String newTrack, BlockingQueue<CoordPoint> newQueue) {
		user  = newUser;
		track = newTrack;
		queue = newQueue;
	}
	
	public String GetUser() {
		return user;
	}
	
	public String GetTrack() {
		return track;
	}	
	
	public String writeQueueXml() {
	    XmlSerializer serializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
	    
	    try {
	        serializer.setOutput(writer);
	        serializer.startDocument("UTF-8", false);
	        
	        serializer.startTag("", "myroad");
	        serializer.startTag("", "info");
	        String localUser = GetUser();
	        if(localUser.length()>0) {
		        serializer.startTag("", "user");
		        serializer.text(GetUser());
		        serializer.endTag("", "user");
	        }
	        serializer.startTag("", "software");
	        serializer.attribute("", "version", "1.3");
	        serializer.startTag("", "name");        
	        serializer.text("MyRoad Tracker");	        
	        serializer.endTag("", "name");	        

	        serializer.startTag("", "author");        
	        serializer.text("Igor Zamyatin");	        
	        serializer.endTag("", "author");	        
	        serializer.endTag("", "software");
	        
	        serializer.startTag("", "time");        
            serializer.text(MRDefaults.GenMyDate(MRDefaults.MY_DATE_FORMAT));	               
	        serializer.endTag("", "time");	        
	        serializer.endTag("", "info");
	        
	        serializer.startTag("", "track");
            serializer.attribute("", "points", "" + queue.size());

	        for (Iterator<CoordPoint> i = queue.iterator(); i.hasNext();) {
	        	CoordPoint cp = (CoordPoint) i.next();
	        	
	            serializer.startTag("", "point");
	            serializer.attribute("", "la", cp.getLa());
	            serializer.attribute("", "lo", cp.getLo());
	            serializer.attribute("", "al", cp.getAl());	            
	            serializer.attribute("", "dt", cp.getDt().replace(' ', 'T')+(cp.getDt().indexOf("Z")>0?"":"Z"));	            
	            serializer.attribute("", "tr", cp.getTr());
	            serializer.attribute("", "nm", cp.getNm());
	            if(cp.getDe()!=null)
	            if(cp.getDe().length()>0)
	            	serializer.text(cp.getDe());
	            serializer.endTag("", "point");	            
	        }
            
	        serializer.endTag("", "track");
	        serializer.endTag("", "myroad");
	        serializer.endDocument();
	        
	        serializer.flush();
	        
	    } catch (Exception e) {
	        //throw new RuntimeException(e);
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " XML writer exception: " + e.getMessage());
			e.printStackTrace();	        
	    } finally {
	    	
		}	        
	        
	    return writer.toString();	
	}
}
