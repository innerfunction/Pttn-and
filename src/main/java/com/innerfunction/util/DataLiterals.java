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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class useful for describing map and list data literals in code.
 * Attached by juliangoacher on 29/05/16.
 */
public class DataLiterals {

    public static class KeyValuePair {
        String key;
        Object value;
        KeyValuePair(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    public static KeyValuePair kv(String key, Object value) {
        return new KeyValuePair( key, value );
    }

    public static Map<String,Object> m(KeyValuePair... kvPairs) {
        Map<String,Object> result = new HashMap<>();
        for( KeyValuePair kvPair : kvPairs ) {
            result.put( kvPair.key, kvPair.value );
        }
        return result;
    }

    public static List<Object> l(Object... items) {
        return Arrays.asList( items );
    }
}
