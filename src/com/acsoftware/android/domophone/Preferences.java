package com.acsoftware.android.domophone;
/*
 Copyright (C) AC SOFTWARE SP. Z O.O.
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

public class Preferences extends PreferenceActivity {
      
	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	// TODO Auto-generated method stub
    	super.onCreate(savedInstanceState);
  
    	Trace.d("------>", Integer.toString( Build.VERSION.SDK_INT));
    	if ( Build.VERSION.SDK_INT < 11 ) {
    		
    		addPreferencesFromResource(R.xml.prefs);
    	} else {
    		getFragmentManager().beginTransaction().replace(android.R.id.content, new _PreferenceFragment()).commit();
    	}
    	

    }

      public static class _PreferenceFragment extends PreferenceFragment
      {
          @Override
          public void onCreate(final Bundle savedInstanceState)
          {
              super.onCreate(savedInstanceState);
              addPreferencesFromResource(R.xml.prefs);
          }
      }
}

