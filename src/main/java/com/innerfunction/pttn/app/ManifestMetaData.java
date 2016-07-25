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
package com.innerfunction.pttn.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.innerfunction.pttn.Property;

import java.util.Map;

/**
 * Attached by juliangoacher on 22/07/16.
 */
public class ManifestMetaData {

    static final String Tag = ManifestMetaData.class.getSimpleName();

    public static void applyTo(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo( packageName, PackageManager.GET_META_DATA );
            Bundle bundle = ai.metaData;
            String metaPrefix = context.getClass().getSimpleName();
            Map<String, Property> properties = Property.getPropertiesForObject( context );
            for( String name : properties.keySet() ) {
                Property property = properties.get( name );
                Class<?> type = property.getType();
                String metaName = String.format( "%s.%s", metaPrefix, name );
                try {
                    if( type == Integer.class ) {
                        Integer defaultValue = (Integer)property.get( context );
                        property.set( context, bundle.getInt( metaName, defaultValue ) );
                    }
                    else if( type == Float.class ) {
                        Float defaultValue = (Float)property.get( context );
                        property.set( context, bundle.getFloat( metaName, defaultValue ) );
                    }
                    else if( type == String.class ) {
                        CharSequence defaultValue = (CharSequence)property.get( context );
                        CharSequence value = bundle.getCharSequence( metaName, defaultValue );
                        if( value != null ) {
                            property.set( context, value.toString() );
                        }
                    }
                }
                catch(Exception e) {
                    Log.w( Tag, String.format( "Reading meta-data item %s: %s", metaName, e.getMessage() ) );
                }
            }
        }
        catch(PackageManager.NameNotFoundException e) {
            Log.w( Tag, e.getMessage() );
        }
    }
}
