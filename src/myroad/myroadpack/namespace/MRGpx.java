package myroad.myroadpack.namespace;

import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class MRGpx {
	final static String GPXVERSION = "1.0";
	
	private XmlSerializer serializer = null;    
    private StringWriter writer = null;
    
	private String track = "";    
	private boolean trkWasClosed = true;
	
	private BlockingQueue<CoordPoint> list;
	
	MRGpx(String newTrack, BlockingQueue<CoordPoint> newList) {
		track = newTrack;
		list = newList;
		//countAll = 0;
	}	
	
	public String GetTrack() {
		return track;
	}	
    
	public void StartGPX() {
		serializer = Xml.newSerializer();
		writer = new StringWriter();		
		try {
	        serializer.setOutput(writer);
	        serializer.startDocument("UTF-8", false);
	        serializer.startTag("", "gpx");
	        serializer.attribute("", "vesrion", GPXVERSION);
	        
	        String localTrack = GetTrack();
	        if(localTrack.length()>0) {
		        serializer.startTag("", "name");
		        serializer.text(localTrack);
		        serializer.endTag("", "name");        	
	        }	        
	                
	        serializer.startTag("", "metadata");
	        serializer.startTag("", "link");
	        serializer.attribute("", "href", MRDefaults.baseURL);
	        serializer.startTag("", "text");
	        serializer.text(MRDefaults.appTitle);
	        serializer.endTag("", "text");
	        serializer.endTag("", "link");
	        serializer.startTag("", "time");	                
            serializer.text(MRDefaults.GenMyDate(MRDefaults.MY_DATE_FORMAT));	        
	        serializer.endTag("", "time");
	        serializer.endTag("", "metadata");
	        
	        serializer.startTag("", "trk"); 	        

	        if(localTrack.length()>0) {
		        serializer.startTag("", "name");
		        serializer.text(localTrack);
		        serializer.endTag("", "name");        	
	        }
	        
	        serializer.startTag("", "trkseg");
	        trkWasClosed = false;
			
	    } catch (Exception e) {

			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();	        
	    }					
	}
	
	public StringWriter EndGPX() {
		try {      			
			if(!list.isEmpty())
			for(int i = 0; i < list.size(); i++ ) {
				
				CoordPoint ccpp = (CoordPoint)list.poll();
				if(ccpp!=null)
					writeXmlAsGPX(ccpp);
			}			

			if(!trkWasClosed) {
		        serializer.endTag("", "trkseg");	        
		        serializer.endTag("", "trk");	        
		        trkWasClosed = true;
			}
		
	        serializer.endTag("", "gpx");
	        serializer.endDocument();	        
	        serializer.flush();

	    } catch (Exception e) {

			Log.e(MRDefaults.LOGTAG, getClass().getName() + " EndGPX Exception: " + e.getMessage());
			e.printStackTrace();	     		
	    } 
	    return writer;
	}	
	
	public void writeXmlAsGPX(CoordPoint cp) {
	    if(cp==null) {
	    	Log.e(MRDefaults.LOGTAG, "Null coord point in writeXmlAsGPX");
	    	return;
	    }
	    int str = 0;
	    try {       	        	
	    	if(cp.getTr().toUpperCase().equals("NOLINE")) {
		        serializer.endTag("", "trkseg");	        
		        serializer.endTag("", "trk");
		        trkWasClosed = true;
		        str++;
		        
	            serializer.startTag("", "wpt");
	            serializer.attribute("", "lat", cp.getLa());
                serializer.attribute("", "lon", cp.getLo());
                str++;
	            
	            if(cp.getAl()!=null)                
	            if(cp.getAl().length()>0) {
	            	serializer.attribute("", "alt", cp.getAl());
	            }
	            str++;

	            if(cp.getNm()!=null)	            
	            if(cp.getNm().length()>0) {
	            	serializer.startTag("", "name");
	            	serializer.text(cp.getNm());
	            	serializer.endTag("", "name");
	            }	  
	            str++;
	            if(cp.getDe()!=null)	            
	            if(cp.getDe().length()>0) {
	            	serializer.startTag("", "desc");
	            	serializer.text(cp.getDe());
	            	serializer.endTag("", "desc");
	            }
	            str++;
	            serializer.startTag("", "time");
	            serializer.text(cp.getDt().replace(' ', 'T')+(cp.getDt().indexOf("Z")>0?"":"Z"));
	            serializer.endTag("", "time");	            
	            serializer.endTag("", "wpt");
	            str++;
	    	} else {
	    		str+=100;
	    		if(trkWasClosed) {
	    			serializer.startTag("", "trk");       	 
			        // 
			        String localTrack = GetTrack();
			        if(localTrack.length()>0) {
				        serializer.startTag("", "name");
				        serializer.text(localTrack);
				        serializer.endTag("", "name");        	
			        }		        
			        
			        serializer.startTag("", "trkseg");	    			
			        trkWasClosed = false;			        
	    		}
	    		str++;
	            serializer.startTag("", "trkpt");
	            serializer.attribute("", "lat", cp.getLa());
	            serializer.attribute("", "lon", cp.getLo());
	            str++;
	            
	            if(cp.getAl()!=null)	            
	            if(cp.getAl().length()>0) {
	            	serializer.attribute("", "alt", cp.getAl());
	            }
	            str++;
	            serializer.startTag("", "time");
	            serializer.text(cp.getDt().replace(' ', 'T')+(cp.getDt().indexOf("Z")>0?"":"Z"));
	            serializer.endTag("", "time");
	            if(cp.getNm()!=null)
	            if(cp.getNm().length()>0) {
	            	serializer.startTag("", "name");
	            	serializer.text(cp.getNm());
	            	serializer.endTag("", "name");
	            }
	            str++;
	            if(cp.getDe()!=null)
	            if(cp.getDe().length()>0) {
	            	serializer.startTag("", "desc");
	            	serializer.text(cp.getDe());
	            	serializer.endTag("", "desc");
	            }
	            str++;
	            
	            serializer.startTag("", "fix");
	            serializer.text("2d");
	            serializer.endTag("", "fix");
	            str++;
	            serializer.endTag("", "trkpt");
	    	}	    	
	    } catch (Exception e) {

			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception " + str + ": " + e.getMessage());
			e.printStackTrace();	        
	    } finally {
		}    
	}
    

}