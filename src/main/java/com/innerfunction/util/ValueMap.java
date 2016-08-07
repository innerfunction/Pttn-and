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

import java.util.Map;

/**
 * A wrapper for a Map instance, providing standard type-specific value accessors.
 * Attached by juliangoacher on 05/05/16.
 */
public class ValueMap {

    private Map values;
    private TypeConversions typeConversions;

    public ValueMap(Map values, TypeConversions typeConversions) {
        this.values = values;
        this.typeConversions = typeConversions;
    }

    public Map getValues() {
        return values;
    }

    public Object resolve(String keyPath) {
        return KeyPath.resolve( keyPath, values );
    }

    public String getString(String keyPath) {
        return getString( keyPath, null );
    }

    public String getString(String keyPath, String defaultValue) {
        String result = defaultValue;
        Object value = resolve( keyPath );
        if( value instanceof String ) {
            result = (String)value;
        }
        else if( value != null ) {
            result = value.toString();
        }
        return result;
    }

    public Number getNumber(String keyPath) {
        return getNumber( keyPath, null );
    }

    public Number getNumber(String keyPath, Number defaultValue) {
        Number result = defaultValue;
        Object value = resolve( keyPath );
        if( value instanceof Number ) {
            result = (Number)value;
        }
        return result;
    }

    public Boolean getBoolean(String keyPath) {
        return getBoolean( keyPath, null );
    }

    public Boolean getBoolean(String keyPath, Boolean defaultValue) {
        Boolean result = defaultValue != null ? defaultValue : Boolean.FALSE;
        Object value = resolve( keyPath );
        if( value instanceof Boolean ) {
            result = (Boolean)value;
        }
        else if( value instanceof Number ) {
            result = ((Number)value).intValue() != 0;
        }
        return result;
    }

    public int getColor(String keyPath, int defaultValue) {
        Object value = resolve( keyPath );
        return value == null ? defaultValue : typeConversions.asColor( value );
    }
}
