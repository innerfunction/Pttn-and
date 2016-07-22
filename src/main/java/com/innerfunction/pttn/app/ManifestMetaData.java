package com.innerfunction.pttn.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.innerfunction.pttn.Property;

import java.util.Map;

/**
 * Created by juliangoacher on 22/07/16.
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
