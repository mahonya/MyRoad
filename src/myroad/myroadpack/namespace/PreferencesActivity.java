package myroad.myroadpack.namespace;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class PreferencesActivity extends PreferenceActivity {

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        EditTextPreference deltasecsPrefs = (EditTextPreference) getPreferenceScreen().findPreference("deltasecs");
        EditTextPreference trackIDPrefs = (EditTextPreference) getPreferenceScreen().findPreference("trackid");
        		
    	OnPreferenceChangeListener pcl = new OnPreferenceChangeListener() {
       
	        public boolean onPreferenceChange(Preference preference, Object newValue) {
	            Boolean rtnval = true;
	            Log.d("my-road", getClass().getName() + " preference.getKey: " + preference.getKey() +
	            		" value = " + newValue.toString());
	            /*
	            if () {
	                final AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity);
	                builder.setTitle("Invalid Input");
	                builder.setMessage("Something's gone wrong...");
	                builder.setPositiveButton(android.R.string.ok, null);
	                builder.show();
	                rtnval = false;
	            }
	            */
	            return rtnval;
	        }
    	};
        
        deltasecsPrefs.setOnPreferenceChangeListener(pcl);        
        trackIDPrefs.setOnPreferenceChangeListener(pcl);
    }
    
}