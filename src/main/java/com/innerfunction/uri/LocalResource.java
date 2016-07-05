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
package com.innerfunction.uri;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A resource used to represent values in the app's local storage.
 * Instances of this class are returned by the local: URI scheme; see LocalScheme.
 */
public class LocalResource extends Resource {

    private static final String LogTag = LocalResource.class.getSimpleName();
    /** The app's local storage. */
    private SharedPreferences preferences;
    /** The name used to refer to the value. */
    private String localName;

    LocalResource(Context context, SharedPreferences preferences, CompoundURI uri) {
        super( context, uri.getName(), uri );
        this.preferences = preferences;
        localName = uri.getName();
    }

    public String getLocalName() {
        return localName;
    }

    public String asString() {
        return preferences.getString( localName, null );
    }

    public Number asNumber() {
        return preferences.getFloat( localName, 0 );
    }

    public Boolean asBoolean() {
        return preferences.getBoolean( localName, false );
    }

    /**
     * Update the value in local storage.
     * @param value A new value to bind to the local name.
     * @return Returns true if the local value was updated.
     */
    public boolean updateWithValue(Object value) {
        SharedPreferences.Editor editor = preferences.edit();
        if( value instanceof Boolean ) {
            editor.putBoolean( localName, (Boolean)value );
        }
        else if( value instanceof Number ) {
            editor.putFloat( localName, ((Number)value).floatValue() );
        }
        else {
            editor.putString( localName, value.toString() );
        }
        if( editor.commit() ) {
            return true;
        }
        Log.w(LogTag, String.format("Failed to write update to shared preferences (%s -> %s)", value, localName ));
        return false;
    }

}