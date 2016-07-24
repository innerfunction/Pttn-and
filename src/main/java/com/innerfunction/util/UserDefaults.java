// Copyright 2016 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.util;

import static com.innerfunction.util.DataLiterals.*;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * A wrapper around a SharedPreferences instance.
 * Named after NSUserDefaults.
 * Attached by juliangoacher on 08/07/16.
 */
public class UserDefaults {

    private SharedPreferences preferences;
    private Map<String,?> values;

    public UserDefaults(Context context) {
        this.preferences = context.getSharedPreferences("pttn", Context.MODE_PRIVATE );
    }

    public UserDefaults(Context context, String name) {
        this.preferences = context.getSharedPreferences( name, Context.MODE_PRIVATE );
    }

    public Object get(String key) {
        if( values == null ) {
            values = preferences.getAll();
        }
        return values.get( key );
    }

    public boolean getBoolean(String key) {
        return preferences.getBoolean( key, false );
    }

    public String getString(String key) {
        return preferences.getString( key, null );
    }

    public void set(String key, Object value) {
        set( m( kv( key, value ) ) );
    }

    public void set(Map<String,Object> values) {
        this.values = null; // Clear any cached getAll() result.
        SharedPreferences.Editor editor = preferences.edit();
        for( String key : values.keySet() ) {
            Object value = values.get( key );
            if( value instanceof String ) {
                editor.putString( key, (String)value );
            }
            else if( value instanceof Boolean ) {
                editor.putBoolean( key, (Boolean)value );
            }
            else if( value instanceof Integer ) {
                editor.putInt( key, (Integer)value );
            }
            else if( value instanceof Float ) {
                editor.putFloat( key, (Float)value );
            }
            else if( value instanceof Long ) {
                editor.putLong( key, (Long)value );
            }
            else if( value == Null.Placeholder ) {
                // Remove null values.
                editor.remove( key );
            }
        }
        editor.commit();
    }

}
