package com.innerfunction.pttn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of the Map interface backed by a List instance.
 * Maps string keys like "1" to the integer 1.
 *
 * Created by juliangoacher on 15/04/16.
 */
public class ListBackedMap implements Map<String,Object> {

    /** An object placeholder for representing null values in the list. */
    static final Object Null = new Object();

    /** The list providing the map data. */
    private List<Object> list;

    public ListBackedMap() {
        this.list = new ArrayList<Object>();
    }

    public ListBackedMap(List<Object> list) {
        this.list = list;
    }

    public List getList() {
        return list;
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return get( key ) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return list.contains( value );
    }

    @Override
    public Set<Entry<String,Object>> entrySet() {
        Set<Entry<String,Object>> entries = new HashSet<>();
        for( int i = 0; i < list.size(); i++ ) {
            entries.add( new ListEntry( i, list.get( i ) ) );
        }
        return entries;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ListBackedMap && ((ListBackedMap)obj).list.equals( list ))
            || (obj instanceof List && obj.equals( list ));
    }

    @Override
    public Object get(Object key) {
        int i = keyToIndex( key );
        Object item = i > 0 && i < size() ? list.get( i ) : null;
        return itemToResult( item );
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        for( int i = 0; i < list.size(); i++ ) {
            keys.add( ((Integer)i).toString() );
        }
        return keys;
    }

    @Override
    public Object put(String key, Object value) {
        Object prevItem = null;
        int i = keyToIndex( key );
        if( i > -1 ) {
            // If the list is shorter than the index to be inserted into then pad the list out
            // will null references.
            for( int j = size(); j < i + 1; j++ ) {
                list.add( Null );
            }
            prevItem = list.get( i );
            list.set( i, value );
        }
        return itemToResult( prevItem );
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
        for( String key : map.keySet() ) {
            put( key, map.get( key ) );
        }
    }

    @Override
    public Object remove(Object key) {
        Object item = null;
        int i = keyToIndex( key );
        if( i > -1 && i < size() ) {
            item = list.get( i );
            list.set( i, Null );
        }
        return itemToResult( item );
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Collection values() {
        return list;
    }

    private Object itemToResult(Object item) {
        return item == Null ? null : item;
    }

    private int keyToIndex(Object key) {
        try {
            return Integer.parseInt( key.toString() );
        }
        catch(Exception e) {}
        return -1;
    }

    static class ListEntry implements Entry<String,Object> {

        private String key;
        private Object value;

        ListEntry(Integer key, Object value) {
            this.key = key.toString();
            this.value = value;
        }

        public boolean equals(Object obj) {
            if( obj instanceof Entry ) {
                Entry e = (Entry)obj;
                return key.equals( e.getKey() ) && value.equals( e.getValue() );
            }
            return false;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public int hashCode() {
            return key.hashCode();
        }

        public Object setValue(Object value) {
            Object result = this.value;
            // NOTE not a complete implementation - doesn't update the source collection.
            this.value = value;
            return result;
        }
    }
}
