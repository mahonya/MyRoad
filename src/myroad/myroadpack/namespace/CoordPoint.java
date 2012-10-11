package myroad.myroadpack.namespace;

//import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/*
 * US vasja PW qwerty NM Москва TR Моё путешествие 
 * DT 2009.02.24 18:17:19 LA N55 44.675 LO E37 35.063 AL 123 DE Я здесь
 */

public class CoordPoint implements Parcelable{
	
	private static final long serialVersionUID = -2218692456704435880L;
	
	private String us = "";
	private String pw = "";
	private String la = "";
	private String lo = "";
	private String al = "";	
	private String dt = "";
	private String tr = "";
	private String nm = "";
	private String de = "";
		
	private boolean isOk = true;
	private boolean isEmpty = true;

	public CoordPoint(String... params) {
		us = params[0];	
		pw = params[1];
		la = params[2];		
		lo = params[3];
		al = params[4];
		dt = params[5];
		tr = params[6];
		nm = params[7];
		de = params[8];			
		
		isEmpty = false;
		isOk = true;
	}
	
	public CoordPoint(Parcel in) {
		readFromParcel(in);
	}	
	
	public String getUs() { return us; }
	public String getPw() { return pw; }	
	public String getLa() { return la; }	
	public String getLo() { return lo; }	
	public String getAl() { return al; }	
	public String getDt() { return dt; }	
	public String getTr() { return tr; }	
	public String getNm() { return nm; }	
	public String getDe() { return de; }	
	
	public boolean getIsOk() { 
		if(!isEmpty) return isOk;
		else return false;
	}
	
	public boolean getIsEmpty() { 
		return isEmpty;		
	}	
	
	public String getBody() {
		return getHTTPBody(); 
	}
	
	public String getSMSBody() {
		String res  = "US " + us + " PW " + pw;
			
		if (nm != null && !"".equals(nm) && nm.length() > 0) {
			res += " NM " +nm;
		}
		if (tr != null && !"".equals(tr) && tr.length() > 0) {
			if(!tr.equals("NOLINE")) res+= " NOLINE ";
			else res += " TR " +tr;
		}
		res += " DT " + dt + " LA " + la + " LO " + lo;
		
		if (al != null && !"".equals(al) && al.length() > 0 && !al.equals("0")) {
			res += " AL " + al;
		}
		
		if (de != null && !"".equals(de) && de.length() > 0) {
			res += " DE ";
		}		

		return res;
	}
	
	public String getHTTPBody() {
		String res  = "us " + us + " pw " + pw;
			
		if (nm != null && !"".equals(nm) && nm.length() > 0) {
			res += " nm " +nm;
		}
		if (tr != null && !"".equals(tr) && tr.length() > 0) {
			if(!tr.equals("NOLINE")) res+= " NOLINE ";
			else res += " tr " +tr;
		}
		res += " dt " + dt + " la " + la + " lo " + lo;
		
		if (al != null && !"".equals(al) && al.length() > 0 && !al.equals("0")) {
			res += " al " + al;
		}
		
		if (de != null && !"".equals(de) && de.length() > 0) {
			res += " de ";
		}		

		return res;
	}	
	
	public String getKey() {
		String sdt = "";
		if(dt!=null) {
			if(dt.length()>3) {
				sdt = dt.substring(1, dt.length()-(dt.indexOf('Z')>0?4:3));
			}
		}
		
		return (""+la+lo+sdt);
	}
	
	public HttpEntity getHttpEntity() {
		HttpEntity ent = null;
		isOk = true;
		
		List<NameValuePair> nameValuePairs; 		
		
		nameValuePairs = new ArrayList<NameValuePair>();
		
		nameValuePairs.add(new BasicNameValuePair("us", us));
		nameValuePairs.add(new BasicNameValuePair("pw", pw));
		nameValuePairs.add(new BasicNameValuePair("la", la));
		nameValuePairs.add(new BasicNameValuePair("lo", lo));
		nameValuePairs.add(new BasicNameValuePair("dt", dt));
		
		if (al != null && !"".equals(al) && al.trim().length() > 0 && !al.equals("0")) {		
			nameValuePairs.add(new BasicNameValuePair("al", al));
		}
		
		if (nm != null && !"".equals(nm) && nm.trim().length() > 0) {
			nameValuePairs.add(new BasicNameValuePair("nm", nm));
		}

		if (tr != null && !"".equals(tr) && tr.trim().length() > 0) {
			if ("NOLINE".equals(tr.toUpperCase())) {
				nameValuePairs.add(new BasicNameValuePair("noline", ""));
				tr = "";
			} else {
				nameValuePairs.add(new BasicNameValuePair("tr", tr));
			}
		}
		
		if (de != null && !"".equals(de) && de.trim().length() > 0) {
			nameValuePairs.add(new BasicNameValuePair("de", de));
		}		
							
		try {
			ent = new UrlEncodedFormEntity(nameValuePairs);
		} catch (UnsupportedEncodingException e) {
			Log.e("my-road", getClass().getName() + " UnsupportedEncodingException: " + e.getMessage());
			e.printStackTrace();
			ent = null;
			isOk = false;
		}			
		
		return ent;
	}
	
	 public boolean equals(final Object obj) {
        if (super.equals(obj)) {
        	CoordPoint cp = (CoordPoint)obj;
        	if(this.getKey().equals(cp.getKey())) return true;
        	return false;
        }
    	CoordPoint cp = (CoordPoint)obj;
    	if(this.getKey().equals(cp.getKey())) return true;       
        return false;
	 }	
	        
	 public int hashCode() {
		 if(isEmpty) return super.hashCode();
		 return (this.getKey()).hashCode();		 
	 }
	 
	public int describeContents() {
		return 0;
	}	 
	 
	public void writeToParcel(Parcel dest, int flags) {
 
		// We just need to write each field into the
		// parcel. When we read from parcel, they
		// will come back in the same order
						
		dest.writeString(us);
		dest.writeString(pw);
		dest.writeString(la);		
		dest.writeString(lo);
		dest.writeString(al);
		dest.writeString(dt);
		dest.writeString(tr);
		dest.writeString(nm);
		dest.writeString(de);
	}	 	         	        
	        
	private void readFromParcel(Parcel in) {
		 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		us = in.readString();
		pw = in.readString();
		la = in.readString();
		lo = in.readString();
		al = in.readString();
		dt = in.readString();
		tr = in.readString();
		nm = in.readString();
		de = in.readString();		
	}
	
	 public static final Parcelable.Creator<CoordPoint> CREATOR =
	    	new Parcelable.Creator<CoordPoint>() {
	            public CoordPoint createFromParcel(Parcel in) {
	                return new CoordPoint(in);
	            }
	 
	            public CoordPoint[] newArray(int size) {
	                return new CoordPoint[size];
	            }
	        };	 
		
	        
	 /*
	 @Override
	 public String toString() {
		 StringBuilder buffer = new StringBuilder();		 	
		 
		 buffer.append("us");
		 buffer.append("=");
		 buffer.append(us);
		 buffer.append(";");
		 
		 buffer.append("pw");
		 buffer.append("=");
		 buffer.append(pw);
		 buffer.append(";");
		 
		 buffer.append("la");
		 buffer.append("=");
		 buffer.append(la);
		 buffer.append(";");
		 
		 buffer.append("lp");
		 buffer.append("=");
		 buffer.append(lo);
		 buffer.append(";");
		 
		 buffer.append("al");
		 buffer.append("=");
		 buffer.append(al);
		 buffer.append(";");
		 
		 buffer.append("dt");
		 buffer.append("=");
		 buffer.append(dt);
		 buffer.append(";");
		 
		 buffer.append("tr");
		 buffer.append("=");
		 buffer.append(tr);
		 buffer.append(";");
		 
		 buffer.append("nm");
		 buffer.append("=");
		 buffer.append(nm);
		 buffer.append(";");
		 
		 buffer.append("de");
		 buffer.append("=");
		 buffer.append(de);	
		 		 
		 return buffer.toString();	 
	 }
	 */
		
}