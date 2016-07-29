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

import java.util.List;
import java.util.Map;

/**
 * A utility for providing key-path access to collections.
 * A key path is a string containing a series of keys separated by full-stops (.). Keys are used to
 * lookup values in Map instances; keys are converted to integers and used to lookup items in
 * List instances.
 * Values can be modified on the fly by using a Modifier instance.
 * Attached by juliangoacher on 26/03/16.
 */
public class KeyPath {

    /**
     * An interface allowing on-the-fly modification of values found along a key path.
     */
    public interface Modifier<T> {
        /**
         * Modify the object that a key will be resolved on.
         * @param key       The current key.
         * @param object    The current object.
         * @param context   An optional object providing context information. Can be null.
         * @return The object that key should be resolved on. Can be null, in which case no object
         * resolution will be done.
         */
        public Object modifyObject(String key, Object object, T context);
        /**
         * Modify the value found mapped to a key.
         * @param key       The key used to lookup the value.
         * @param value     The value to modify. Can be null.
         * @param context   An optional object providing context information. Can be null.
         * @return The modified value. Can be null.
         */
        public Object modifyValue(String key, Object value, T context);
    }

    /**
     * Resolve a key path on a root object.
     * No value modifications are performed.
     * @param keyPath   The key path to resolve.
     * @param rootValue The root object.
     * @return The resolved value, or null if the value can't be resolved.
     */
    public static Object resolve(String keyPath, Object rootValue) {
        return resolve( keyPath, rootValue, null );
    }

    /**
     * Resolve a key path on a root object.
     * @param keyPath   The key path to resolve.
     * @param rootValue The root object.
     * @param modifier  An object used to modify objects and values as the key path is resolved.
     *                  Can be null, in which case no modifications are done.
     * @return The resolved value, or null if the value can't be resolved.
     */
    public static <T> Object resolve(String keyPath, Object rootValue, Modifier<T> modifier) {
        return resolve( keyPath, rootValue, null, modifier );
    }

    /**
     * Resolve a key path on a root object.
     * @param keyPath   The key path to resolve.
     * @param rootValue The root object.
     * @param context   An optional object providing context information. Can be null.
     * @param modifier  An object used to modify objects and values as the key path is resolved.
     *                  Can be null, in which case no modifications are done.
     * @return The resolved value, or null if the value can't be resolved.
     */
    public static <T> Object resolve(String keyPath, Object rootValue, T context, Modifier<T> modifier) {
        String[] keys = keyPath.split("\\.");
        int i = 0;
        Object value = rootValue;
        while( value != null && i < keys.length ) {
            if( modifier != null ) {
                value = modifier.modifyObject( keys[i], value, context );
            }
            if( value instanceof Map) {
                // Attempt to read the next value using the key as the map key.
                value = ((Map)value).get( keys[i] );
            }
            else if( value instanceof List) {
                try {
                    // Attempt to read the next value using the key as the list index.
                    int key = Integer.valueOf( keys[i] );
                    value = ((List)value).get( key );
                }
                catch(NumberFormatException e) {
                    value = null;
                }
            }
            else {
                // Try using reflection to read a value from the object.
                Map<String,Property> properties = Property.getPropertiesForObject( value );
                Property property = properties.get( keys[i] );
                if( property != null ) {
                    value = property.get( value );
                }
                else value = null;
            }
            if( modifier != null ) {
                value = modifier.modifyValue( keys[i], value, context );
            }
            i++;
        }
        return value;
    }

    /**
     * Resolve a key path reference and return its value as a string.
     */
    public static final String getValueAsString(String keyPath, Object rootValue) {
        Object value = resolve( keyPath, rootValue );
        return value == null ? null : value.toString();
    }

    /**
     * Resolve a key path reference and return its value as an integer.
     */
    public static final int getValueAsInt(String keyPath, Object rootValue) {
        Object value = resolve( keyPath, rootValue );
        return value instanceof Number ? ((Number)value).intValue() : null;
    }

    /**
     * Resolve a key path reference and return its value as a boolean.
     */
    public static final boolean getValueAsBoolean(String keyPath, Object rootValue) {
        Object value = resolve( keyPath, rootValue );
        if( value instanceof Boolean ) {
            return (Boolean)value;
        }
        if( value instanceof Number ) {
            // Any non-zero value is true.
            return ((Number)value).intValue() != 0;
        }
        if( value instanceof String ) {
            return "true".equals( ((String)value).toLowerCase() );
        }
        return false;
    }

}
