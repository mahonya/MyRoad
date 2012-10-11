package myroad.myroadpack.namespace;

import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class MRKml {
	
    private XmlSerializer serializer = null;    
    private StringWriter writer = null;
    
	private String track = "";
	private int countAll = 0;
	
	private BlockingQueue<CoordPoint> list;
	
	MRKml(String newTrack, BlockingQueue<CoordPoint> newList) {
		track = newTrack;
		list = newList;
		countAll = 0;
	}

	public String GetTrack() {
		return track;
	}	
	
	public void StartKML() {
		serializer = Xml.newSerializer();
		writer = new StringWriter();		
		try {
	        serializer.setOutput(writer);
	        serializer.startDocument("UTF-8", false);	        	        
	        serializer.startTag("", "kml");
	        serializer.startTag("", "Document");	        	        	        			
	    } catch (Exception e) {

			Log.e(MRDefaults.LOGTAG, getClass().getName() + " Exception: " + e.getMessage());
			e.printStackTrace();	        
	    }					
	}
	
	public StringWriter EndKML() {
		try {      
	        String localTrack = GetTrack();
			if(!list.isEmpty())
			{
				for(int i = 0; i < list.size(); i++ ) {
					CoordPoint ccpp = (CoordPoint)list.poll();
					if(ccpp!=null)
						writeXmlAsKML(ccpp);
				}

	            serializer.startTag("", "Placemark");
		        if(localTrack.length()>0) {
			        serializer.startTag("", "name");
			        serializer.text(localTrack);
			        serializer.endTag("", "name");        	
		        }	            
	            
				serializer.startTag("", "LineString");	            
				serializer.startTag("", "coordinates");				
				for(int i = 0; i < list.size(); i++ ) {
					CoordPoint ccpp = (CoordPoint)list.peek();
					if(ccpp!=null) {
						serializer.text(ccpp.getLo()+","+ccpp.getLa());
					}
				}
				serializer.endTag("", "coordinates");
				serializer.endTag("", "LineString");					        
	            serializer.endTag("", "Placemark");				
			}

	        serializer.endTag("", "Document");	        
	        serializer.endTag("", "kml");
	        serializer.endDocument();	        	        
	        serializer.flush();
	        

	    } catch (Exception e) {
	
			Log.e(MRDefaults.LOGTAG, getClass().getName() + " End KML Exception: " + e.getMessage());
			e.printStackTrace();	        
	    }
	    
        return writer;	    
	}		
	
	public void writeXmlAsKML(CoordPoint cp) {
	    if(cp==null) {
	    	Log.e(MRDefaults.LOGTAG, "Null coord point in writeXmlAsKML");
	    	return;
	    }
	    int str = 0;
	    try {       
	        	
            serializer.startTag("", "Placemark");
            str++;
            
            if(cp.getNm().length()>0) {
            	serializer.startTag("", "name");
            	serializer.text(cp.getNm());
            	serializer.endTag("", "name");
            }
            
            str++;
            if(cp.getDe()!=null)
            if(cp.getDe().trim().length()>0) {
            	serializer.startTag("", "description");
            	serializer.text(cp.getDe()); // cdsect is not worked!
            	serializer.endTag(""	, "description");
            }            
            
            str++;
            serializer.startTag("", "LookAt");
            serializer.attribute("", "id", ""+(countAll+1));
            serializer.startTag("", "longitude");
            serializer.text(cp.getLo());
            serializer.endTag("", "longitude");                                   
            serializer.startTag("", "latitude");
            serializer.text(cp.getLa());            
            serializer.endTag("", "latitude");
            if(cp.getAl()!=null)
            if(cp.getAl().trim().length()>0) {           
            serializer.startTag("", "altitude");
            serializer.text(cp.getAl());            
            serializer.endTag("", "altitude");
            }           
            serializer.endTag("", "LookAt");
            
            str++;
            serializer.startTag("", "Point");
            serializer.startTag("", "coordinates");
            serializer.text(cp.getLo()+","+cp.getLa()+","+cp.getAl());
            serializer.endTag("", "coordinates");
            serializer.endTag("", "Point");
            
            serializer.startTag("", "TimeStamp");
            serializer.startTag("", "when");
            serializer.text(cp.getDt().replace(' ', 'T')+(cp.getDt().indexOf("Z")>0?"":"Z"));
            serializer.endTag("", "when");
            serializer.endTag("", "TimeStamp");        
                        
            serializer.endTag("", "Placemark");

	    } catch (Exception e) {

			Log.e(MRDefaults.LOGTAG, getClass().getName() + "writeXmlAsKML " + str + 
					" Exception: " + e.getMessage());
			e.printStackTrace();	        
	    } finally {
		}    
	}		
}