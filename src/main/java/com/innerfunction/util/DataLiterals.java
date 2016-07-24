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
