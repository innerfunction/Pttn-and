package com.innerfunction.pttn;

import android.util.Log;
import android.util.LruCache;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a configurable property member of a class.
 * A configurable property at a minimum has a setter method with a name in the format setXxx, where
 * the property name is xxx; e.g a setBackgroundColor(...) method corresponds to a property named
 * backgroundColor.
 * Configurable properties may also optionally have getter methods in the form getXxx. Boolean
 * properties also support setters in the format isXxx or hasXxx.
 *
 * Created by juliangoacher on 30/03/16.
 */
public class Property {

    static final String Tag = Property.class.getSimpleName();

    /** The property name, e.g. backgroundColor. */
    private String name;
    /** The property type class. */
    private Class<?> type;
    /** The property's setter method. */
    private Method setter;
    /** The property's getter method. Can be null. */
    private Method getter;

    private Property(String baseName, Map<String,Method> methods) {
        // PROFILING NOTE The string operations in this method - up to 5 separate strings are
        // constructed - incur a significant CPU overhead, so a single string builder is used to
        // generate them all.
        // NOTE Ideally in the code below, the string builder could be used as a map key without
        // first converting to string; but the string builder implementation's hashCode and equals
        // methods do not support this behaviour; consider whether worthwhile implementing a
        // string builder subclass that does support this. (Note that StringBuilder is final, so
        // a complete new class would be necessary).
        StringBuilder sb = new StringBuilder();
        sb.append("set");
        sb.append( baseName );
        this.setter = methods.get( sb.toString() );
        this.type = setter.getParameterTypes()[0];
        // Try to find a getter.
        sb.replace( 0, 1, "g"); // e.g. [s]etXxx -> [g]etXxx
        getter = methods.get( sb.toString() );
        if( getter == null && type == Boolean.class ) {
            sb.replace( 0, 3, "is"); // e.g. [get]Xxx -> [is]Xxx
            getter = methods.get( sb.toString() );
            if( getter == null ) {
                sb.replace( 0, 2, "has"); // e.g. [is]Xxx -> [has]Xxx
                getter = methods.get( sb.toString() );
                sb.delete( 0, 3 ); // remove 'has'
            }
            else sb.delete( 0, 2 ); // remove 'is'
        }
        else sb.delete( 0, 3 ); // remove 'get'
        // By this point, all prefixes have been removed and sb should contain just baseName.
        // Convert the base name, in format Xxx, to the property name in format xxx
        // e.g. BackgroundColor -> backgroundColor
        sb.replace( 0, 1, baseName.substring( 0, 1 ).toLowerCase() );
        this.name = sb.toString();
        /*
        this.name = baseName.substring( 0, 1 ).toLowerCase()+baseName.substring( 1 );
        this.setter = methods.get("set"+baseName );
        this.type = setter.getParameterTypes()[0];
        getter = methods.get("get"+baseName );
        if( getter == null && type == Boolean.class ) {
            getter = methods.get("is"+baseName );
            if( getter == null ) {
                getter = methods.get("has"+baseName );
            }
        }
        */
    }

    /** Constructor for use by Property subclasses. */
    protected Property(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    /** Test whether the property will accept an object value of any type. */
    public boolean isAnyType() {
        return type == Object.class;
    }

    public String getTypeClassName() {
        return type.getClass().getCanonicalName();
    }

    /**
     * Get the generic parameter types for the setter method for a collections property.
     * For example, for a Map property declared as Map<String,Date>, the generic parameters
     * are [ String, Date ].
     * Returns null if no generic parameter type info is available.
     */
    public Type[] getGenericParameterTypeInfo() {
        Type genericArgType = setter.getGenericParameterTypes()[0];
        if( genericArgType instanceof ParameterizedType ) {
            return ((ParameterizedType)genericArgType).getActualTypeArguments();
        }
        return null;
    }

    /**
     * Test whether the property is assignable from another type.
     */
    public boolean isAssignableFrom(Class otherType) {
        return type.isAssignableFrom( otherType );
    }

    /**
     * Test whether the property is a specific type.
     */
    public boolean isType(Class otherType) {
        return type == otherType;
    }

    /**
     * Set the property on an object with the specified value.
     * Returns true if the value was set.
     */
    public boolean set(Object object, Object value) {
        try {
            setter.invoke( object, value );
            return true;
        }
        catch(Exception e) {
            // Unable to set value.
        }
        return false;
    }

    /**
     * Get the property on an object.
     * Returns null if the property value is null, or if the property does not have a getter method,
     * or if an error occurs when accessing the property value.
     */
    public Object get(Object object) {
        if( getter != null ) {
            try {
                return getter.invoke( object );
            }
            catch(Exception e) {
                // Unable to get value.
            }
        }
        return null;
    }

    /**
     * A cache of object properties by class name.
     * Used to cache the results of getPropertiesForObject(..). The Class.getMethods() call can be
     * CPU intensive; however, it is not desirable to cache all results for all classes as this
     * could swap CPU overhead for memory overhead. Instead, an LRU cache with a smallish size (of
     * 40 items) is kept.
     */
    static final LruCache<Class,Map<String,Property>> ObjectPropertiesByClass = new LruCache<>( 40 );

    /**
     * Get the configurable properties for an object.
     * Returns a map of Property instances keyed by property name.
     * @param object    The object whose properties are needed.
     * @return A map of object properties.
     */
    public static Map<String,Property> getPropertiesForObject(Object object) {
        Class<?> cl = object.getClass();
        // Check for a cached result.
        Map<String,Property> properties = ObjectPropertiesByClass.get( cl );
        if( properties == null ) {
            // Cache miss.
            Log.d( Tag, String.format("getPropertiesForObject(%s) cache miss", cl.getCanonicalName()));
            // Build a map of all the class' methods, and a list of property base names.
            Map<String, Method> methods = new HashMap<>();
            List<String> baseNames = new ArrayList<>();
            for( Method method : cl.getMethods() ) {
                String methodName = method.getName();
                if( methodName.startsWith( "set" ) ) {
                    Class[] argTypes = method.getParameterTypes();
                    if( argTypes.length == 1 ) {
                        baseNames.add( methodName.substring( 3 ) );
                    }
                }
                methods.put( methodName, method );
            }
            // Generate a map of properties.
            properties = new HashMap<>();
            for( String baseName : baseNames ) {
                Property property = new Property( baseName, methods );
                properties.put( property.name, property );
            }
            // Add result to cache.
            synchronized( ObjectPropertiesByClass ) {
                ObjectPropertiesByClass.put( cl, properties );
            }
        }
        return properties;
    }
}
