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
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.util.Map;

/**
 * The handler for the local: URI scheme.
 * The local: scheme allows URIs to reference values in the app's local storage. On Android, the
 * app's SharedPreferences is used as local storage.
 * Values referenced using the local: scheme are returned wrapped in a LocalResource object. This
 * object can be used to perform updates on local values.
 */
public class LocalScheme implements URIScheme {

    private static final String LogTag = LocalScheme.class.getSimpleName();

    /** The app context. */
    private Context context;
    /** Local storage. */
    private SharedPreferences preferences;

    public LocalScheme(Context context) {
        this.context = context;
        ApplicationInfo ainfo = context.getApplicationInfo();
        String prefsName = String.format("local.%s", ainfo.processName );
        Log.i( LogTag, String.format("Using shared preferences name %s", prefsName ) );
        preferences = context.getSharedPreferences( prefsName, 0 );
    }

    /**
     * Dereference a local: URI.
     * @param uri The URI to be dereferenced. The URI's name is used as the local value name.
     * @param params Not used by this scheme.
     * @return A LocalResource object encapsulating the local value. Note that a resource is
     * returned even if no value is currently mapped to the local name.
     */
    @Override
    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        return new LocalResource( context, preferences, uri );
    }

}
