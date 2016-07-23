package com.innerfunction.pttn;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class responsible for object configuration.
 * Performs mapping of configuration values to object properties. Instantiates objects from
 * configurations and injects property values.
 *
 * Created by juliangoacher on 15/04/16.
 */
public class ObjectConfigurer {

    static final String Tag = ObjectConfigurer.class.getSimpleName();

    /** An object container. */
    private Container container;
    /** The container's properties. */
    private Properties containerProperties;
    /** Internal metrics: Number of properties (objects and primitives) configured. */
    private int configuredPropertyCount = 0;
    /** Internal metrics: Number of objects (i.e non-primitives) configured. */
    private int configuredObjectCount = 1;

    /**
     * Initialize a configurer with its container.
     * @param container The container to be configured.
     */
    public ObjectConfigurer(Container container) {
        this.container = container;
        this.containerProperties = new ContainerProperties( container );
    }

    /** Perform the container configuration. */
    public void configureWith(Configuration configuration) {
        configure( container, null, configuration, "" );
    }

    /**
     * Configure a named object of the container.
     * @param name          A property name.
     * @param configuration The container configuration.
     * @return
     */
    public Object configureNamedObject(String name, Configuration configuration) {
        Object named = buildPropertyValue( name, containerProperties, configuration, "" );
        if( named != null ) {
            injectPropertyValue( name, containerProperties, named );
        }
        return named;
    }

    /**
     * Configure an object.
     * @param object        The object to configure.
     * @param memberType    Default type for collection members. Can be null for non-collections.
     * @param configuration The object configuration.
     * @param kpPrefix      Key path prefix, i.e. the key path within the container to the object
     *                      being configured. Used for logging purposes.
     */
    public void configure(Object object, Class<?> memberType, Configuration configuration, String kpPrefix) {
        // Start the object configuration.
        if( object instanceof IOCContainerAware ) {
            ((IOCContainerAware)object).beforeIOCConfigure( configuration );
        }
        // Configure the object.
        if( object instanceof Configurable ) {
            ((Configurable)object).configure( configuration, container );
        }
        else {
            // Resolve a property set for the object about to be configured.
            Properties properties = getObjectProperties( object, memberType );
            // Iterate over the each property named in the configuration and try building and then
            // injecting a property value into the object being configured.
            for( String name : configuration.getValueNames() ) {
                String propName = normalizePropertyName( name ); // Check for reserved names.
                if( propName != null ) {
                    // Build a property value from the configuration.
                    Object value = buildPropertyValue( propName, properties, configuration, kpPrefix );
                    // If property value then inject into the object property.
                    if( value != null ) {
                        injectPropertyValue( propName, properties, value );
                    }
                    configuredPropertyCount++;
                }
            }
        }
        configuredObjectCount++;
        // Post configuration.
        if( object instanceof IOCContainerAware ) {
            Object objectKey = new ObjectKey( object );
            if( container.hasPendingValueRefsForObjectKey( objectKey ) ) {
                container.recordPendingValueObjectConfiguration( objectKey, configuration );
            }
            else {
                ((IOCContainerAware)object).afterIOCConfigure( configuration );
            }
        }
        container.doPostConfiguration( object );
    }

    /**
     * Try to build a property value from its configuration.
     * TODO: Rename this to 'resolvePropertyValue' - because sometimes in-place values are returned.
     * @param propName      The name of the property being built.
     * @param properties    The set of properties of the object being configured.
     * @param configuration The object configuration; should contain a configuration for the named
     *                      property.
     * @param kpPrefix      The key path of the object being configured.
     * @return The property value built from the configuration, or null if no value can be resolved.
     */
    private Object buildPropertyValue(String propName, Properties properties, Configuration configuration, String kpPrefix) {
        Object value = null;

        Class<?> propType = properties.getPropertyType( propName );
        // If no property type info then can't process any further, return empty handed.
        if( propType == null ) {
            return null;
        }

        // First, check to see if the property belongs to one of the standard types used to
        // represent primitive configurable values. These values are different to other
        // non-primitive types, in that (1) it's generally possible to convert values between them,
        // and (2) the code won't recursively perform any additional configuration on the values.
        switch( getStandardTypeForClass( propType ) ) {
        case Boolean:
            value = configuration.getValueAsBoolean( propName );
            break;
        case Number:
            Number number = configuration.getValueAsNumber( propName );
            if( propType == Integer.class ) {
                value = number.intValue();
            }
            else if( propType == Float.class ) {
                value = number.floatValue();
            }
            else if( propType == Double.class ) {
                value = number.doubleValue();
            }
            else {
                value = number;
            }
            break;
        case String:
            value = configuration.getValueAsString( propName );
            break;
        case Date:
            value = configuration.getValueAsDate( propName );
            break;
        case Drawable:
            value = configuration.getValueAsImage( propName );
            break;
        case Color:
            value = configuration.getValueAsColor( propName );
            break;
        case Configuration:
            value = configuration.getValueAsConfiguration( propName );
            break;
        case JSONData:
            // Properties which require raw JSON data need to be declared using the JSONObject
            // or JSONArray types, as appropriate. This is intended as an optimization -
            // particularly when initializing a property with a large-ish data set - as
            // the configurer will not attempt to further process the configuration data.
            value = configuration.getValueAsJSONData( propName );
            break;
        }
    /*
        // First check for a primitive value. A primitive is any any value whose properties won't
        // be recursively processed by the configurer. This category includes standard Java
        // primitives - Number, String etc.; and other useful types such as Date and Drawable.
        // The base JSON types - object and array - are also included in this category.
        if( propType != Object.class ) {
            if( propType.isAssignableFrom( Boolean.class ) ) {
                value = configuration.getValueAsBoolean( propName );
            }
            else if( propType.isAssignableFrom( Number.class ) ) {
                Number number = configuration.getValueAsNumber( propName );
                if( propType == Integer.class ) {
                    value = number.intValue();
                }
                else if( propType == Float.class ) {
                    value = number.floatValue();
                }
                else if( propType == Double.class ) {
                    value = number.doubleValue();
                }
                else {
                    value = number;
                }
            }
            else if( propType.isAssignableFrom( String.class ) ) {
                value = configuration.getValueAsString( propName );
            }
            else if( propType.isAssignableFrom( Date.class ) ) {
                value = configuration.getValueAsDate( propName );
            }
            else if( propType.isAssignableFrom( Drawable.class ) ) {
                value = configuration.getValueAsImage( propName );
            }
            else if( propType.isAssignableFrom( Color.class ) ) {
                value = configuration.getValueAsColor( propName );
            }
            else if( propType.isAssignableFrom( Configuration.class ) ) {
                value = configuration.getValueAsConfiguration( propName );
            }
            else if( propType == JSONObject.class || propType == JSONArray.class ) {
                // Properties which require raw JSON data need to be declared using the JSONObject
                // or JSONArray types, as appropriate. This is intended as an optimization -
                // particularly when initializing a property with a large-ish data set - as
                // the configurer will not attempt to further process the configuration data.
                value = configuration.getValueAsJSONData( propName );
            }
        }
        */
        // If value is still null then the property is not a primitive or JSON data type. Try to
        // resolve a new value from the supplied configuration.
        // The configuration may contain a mixture of object definitions and fully instantiated
        // objects. The configuration's 'natural' representation will distinguish between these,
        // return a Configuration instance for object definitions and the actual object instance
        // otherwise.
        // When an object definition is returned, the property value is resolved according to the
        // following order of precedence:
        // 1. A configuration which supplies an instantiation hint - e.g. *type, *and-class or
        //    *factory - and which successfully yields an object instance always takes precedence
        //    over other possible values;
        // 2. Next, any in-place value found by reading from the object property being configured;
        // 3. Finally, a value created by attempting to instantiate the declared type of the
        //    property being configured (i.e. the inferred type).
        if( value == null ) {
            // Fetch the configuration value's natural representation.
            value = configuration.getNaturalValue( propName );
            if( value instanceof Configuration ) {
                // The natural value contains a (potential) object definition, so attempt to
                // resolve the value from it.
                Configuration valueConfig = (Configuration)value;

                // Try asking the container to build a new object using the configuration. This
                // will only work if the configuration contains an instantiation hint (e.g. *type,
                // *factory etc.) and will return a non-null, fully-configured object if successful.
                value = container.buildObject( valueConfig, getKeyPath( kpPrefix, propName ), true );
                if( value == null ) {
                    // Couldn't build a value, so see if the object already has a value in-place.
                    value = properties.getPropertyValue( propName );
                    if( value != null ) {
                        // Apply configuration proxy wrapper, if any defined, to the in-place value.
                        value = IOCProxyLookup.applyConfigurationProxyWrapper( value );
                    }
                    else if( propType != Object.class ) {
                        // No in-place value, so try inferring a value type from the property
                        // information, and then try to instantiate that type as the new value.
                        // (Note that the container method will return a configuration proxy for
                        // those classes which require one.)
                        String className = propType.getCanonicalName();
                        try {
                            value = container.newInstanceForClassNameAndConfiguration( className, valueConfig );
                        }
                        catch(Exception e) {
                            Log.e( Tag, String.format("Error creating new instance of inferred type %s", className ), e );
                        }
                    }
                    // If we now have either an in-place or inferred type value by this point, then
                    // continue by configuring the object with its configuration.
                    if( value != null ) {
                        // Maps are configured the same as object instances, but properties are
                        // mapped to map entries instead of properties of the map class.
                        // Note that by this point, lists are presented as maps (see the
                        // ListIOCProxy class below).
                        Class<?> memberType = null;
                        if( value instanceof Map ) {
                            memberType = properties.getMapPropertyValueTypeParameter( propName );
                        }
                        // Recursively configure the value.
                        configure( value, memberType, valueConfig, getKeyPath( kpPrefix, propName ) );
                    }
                }
            }
        }
        return value;
    }

    /**
     * Inject a value into an object property.
     * @param propName      The name of the property being configured.
     * @param properties    Information about the properties of the object being configured.
     * @param value         The value to inject.
     * @return Returns the value injected into the object property.
     */
    public Object injectPropertyValue(String propName, Properties properties, Object value) {
        Object object = properties.getPropertyOwner();
        // Notify object aware values that they are about to be injected into the object under the
        // current property name.
        // NOTE: This happens at this point - instead of after the value injection - so that value
        // proxies can receive the notification. It's more likely that proxies would implement this
        // protocol than the values they act as proxy for (i.e. because proxied values are likely
        // to be standard platform classes).
        if( value instanceof IOCObjectAware ) {
            ((IOCObjectAware)value).notifyIOCObject( object, propName );
        }
        // If value is a config proxy then unwrap the underlying value
        if( value instanceof IOCProxy ) {
            value = ((IOCProxy)value).unwrapValue();
        }
        // If value is a pending then defer operation until later.
        if( value instanceof PendingNamed ) {
            // Record the current property and object info, but skip further processing. The
            // property value will be set once the named reference is fully configured, see
            // Container.buildNamedObject()
            PendingNamed pending = (PendingNamed)value;
            pending.setKey( propName );
            pending.setConfigurationContext( properties, this );
            container.incPendingValueRefCountForPendingObject( pending );
        }
        else if( value != null ) {
            // Set the object property. Note that the Property instance returned by
            // getPropertyInfo() also handles setting of member items when object is a
            // collection.
            Class<?> propType = properties.getPropertyType( propName );
            if( propType != null && propType.isAssignableFrom( value.getClass() ) ) {
                // Standard object property reference.
                properties.setPropertyValue( propName, value );
            }
        }
        return value;
    }

    /**
     * Return a set of named properties for an object.
     */
    private Properties getObjectProperties(Object object, Class<?> memberType) {
        if( object == container ) {
            return containerProperties;
        }
        if( object instanceof Map ) {
            return new MapProperties( (Map<Object,?>)object, memberType );
        }
        return new ObjectProperties( object );
    }

    /**
     * Normalize a property name by removing any *and- prefix.
     * Returns null for reserved names (e.g. *type etc.)
     */
    private String normalizePropertyName(String propName) {
        if( propName.charAt( 0 ) == '*' ) {
            if( propName.startsWith("*and-") ) {
                // Strip *and- prefix from names
                propName = propName.substring( 5 );
                // Don't process class names.
                if( "class".equals( propName ) ) {
                    propName = null;
                }
            }
            else {
                propName = null; // Skip all other reserved names.
            }
        }
        return propName;
    }

    /**
     * Return a new key path by appending a property name to a prefix.
     * @param prefix    A key path acting as a prefix. Can be an empty string.
     * @param name      A property name to append to the prefix.
     * @return A new key path.
     */
    private static String getKeyPath(final String prefix, final String name) {
        return prefix.length() > 0 ? prefix+"."+name : "#"+name;
    }

    /**
     * A cache of classes to standard types.
     * Used to cache the results of getStandardTypeForClass(..).
     */
    static final LruCache<Class,StandardTypes> StandardTypesByClass = new LruCache<>( 50 );

    /** Enumeration of standard configuration types. */
    public enum StandardTypes { Boolean, Number, String, Date, Drawable, Color, Configuration, JSONData, Other };

    /**
     * Get the standard type value for a class.
     * The standard types are an optimization to the core configuration cycle. Before each property
     * is configured, the configurer checks whether the property type is one of the standard
     * primitive types (listed above). This potentially requires multiple isAssignableFrom() tests,
     * which this code attempts to optimize by cacheing the test result for each class.
     * NOTE: Need for this code should be reviewed; it's contribution to the performance of the
     * configuration code is probably minimal; on the other hand, it maybe? contributes to code
     * readability.
     */
    static final StandardTypes getStandardTypeForClass(Class<?> clss) {
        StandardTypes stdType = StandardTypesByClass.get( clss );
        if( stdType == null ) {
            if( clss == Object.class ) {
                stdType = StandardTypes.Other;
            }
            else if( clss.isAssignableFrom( Boolean.class ) ) {
                stdType = StandardTypes.Boolean;
            }
            else if( clss.isAssignableFrom( Number.class ) ) {
                stdType = StandardTypes.Number;
            }
            else if( clss.isAssignableFrom( String.class ) ) {
                stdType = StandardTypes.String;
            }
            else if( clss.isAssignableFrom( Date.class ) ) {
                stdType = StandardTypes.Date;
            }
            else if( clss.isAssignableFrom( Drawable.class ) ) {
                stdType = StandardTypes.Drawable;
            }
            else if( clss.isAssignableFrom( Color.class ) ) {
                stdType = StandardTypes.Color;
            }
            else if( clss.isAssignableFrom( Configuration.class ) ) {
                stdType = StandardTypes.Configuration;
            }
            else if( clss == JSONObject.class || clss == JSONArray.class ) {
                stdType = StandardTypes.JSONData;
            }
            else {
                stdType = StandardTypes.Other;
            }
            synchronized( StandardTypesByClass ) {
                StandardTypesByClass.put( clss, stdType );
            }
        }
        return stdType;
    }

    public int getConfiguredPropertyCount() {
        return configuredPropertyCount;
    }

    public int getConfiguredObjectCount() {
        return configuredObjectCount;
    }

    /**
     * An interface for presenting information about the configurable properties of an object.
     * @param <T>
     */
    public interface Properties<T> {
        /** Get the object whose properties are being represented. */
        Object getPropertyOwner();
        /** Get type information for a named property. */
        Class<?> getPropertyType(String name);
        /** Get the generic type information for a map value. */
        Class<?> getMapPropertyValueTypeParameter(String name);
        /** Get a named property value. */
        T getPropertyValue(String name);
        /** Set a named property value. */
        boolean setPropertyValue(String name, T value);
    }

    /**
     * A class encapsulating information about an object's properties.
     */
     static class ObjectProperties implements Properties {
        /** The property owner. */
        Object object;
        /** The named properties of the owner. */
        Map<String,Property> properties;

        ObjectProperties(Object object) {
            this.object = object;
            this.properties = Property.getPropertiesForObject( object );
        }
        @Override
        public Object getPropertyOwner() {
            return object;
        }
        @Override
        public Class<?> getPropertyType(String name) {
            Property property = properties.get( name );
            return property != null ? property.getType() : null;
        }
        @Override
        public Class<?> getMapPropertyValueTypeParameter(String name) {
            Property property = properties.get( name );
            if( property != null ) {
                Type[] typeInfo = property.getGenericParameterTypeInfo();
                if( typeInfo.length > 1 && typeInfo[1] instanceof Class ) {
                    return (Class<?>)typeInfo[1];
                }
            }
            return Object.class;
        }
        @Override
        public Object getPropertyValue(String name) {
            Property property = properties.get( name );
            return property != null ? property.get( object ) : null;
        }
        @Override
        public boolean setPropertyValue(String name, Object value) {
            Property property = properties.get( name );
            if( property != null ) {
                property.set( object, value );
                return true;
            }
            return false;
        }
    }

    /**
     * A class encapsulating information about a map's configurable properties.
     * The configurable properties of a map correspond to the map's entries.
     * @param <T>
     */
    static class MapProperties<T> implements Properties<T> {
        /** The map which owns the properties. */
        Map<Object,T> map;
        /**
         * The type to infer for each of the map's properties.
         * This information is inferred from the map's generic type parameters.
         */
        Class<?> inferredMemberType;

        MapProperties(Map<Object,T> map, Class<?> inferredMemberType) {
            this.map = map;
            this.inferredMemberType = inferredMemberType;
        }
        @Override
        public Object getPropertyOwner() {
            return map;
        }
        @Override
        public Class<?> getPropertyType(String name) {
            return inferredMemberType;
        }
        @Override
        public Class<?> getMapPropertyValueTypeParameter(String name) {
            return Object.class;
        }
        @Override
        public T getPropertyValue(String name) {
            return map.get( name );
        }
        @Override
        public boolean setPropertyValue(String name, T value) {
            map.put( name, value );
            return true;
        }
    }

    /**
     * A class encapsulating information about a container's configurable properties.
     * The configurable properties of a container correspond to its named properties.
     */
    static class ContainerProperties extends ObjectProperties {
        /** The container owner of the properties. */
        Container container;

        ContainerProperties(Container container) {
            super( container );
            this.container = container;
        }
        @Override
        public Object getPropertyOwner() {
            return container;
        }
        @Override
        public Class<?> getPropertyType(String name) {
            Class<?> type = super.getPropertyType( name );
            return type != null ? type : Object.class;
        }
        @Override
        public Class<?> getMapPropertyValueTypeParameter(String name) {
            return Object.class;
        }
        @Override
        public Object getPropertyValue(String name) {
            Object value = super.getPropertyValue( name );
            if( value == null ) {
                value = container.getNamed( name );
            }
            return !(value instanceof PendingNamed) ? value : null;
        }
    }

    /**
     * A configuration proxy for List instances.
     * Provides a Map based interface for configuring List instances.
     */
    static class ListIOCProxy extends ListBackedMap implements IOCProxy {
        @Override
        public void initializeWithValue(Object value) {
            if( value instanceof List ) {
                List list = (List)value;
                for( int i = 0; i < list.size(); i++ ) {
                    put( Integer.toString( i ), list.get( i ) );
                }
            }
        }
        @Override
        public Object unwrapValue() {
            return getList();
        }
    }

    /**
     * A configuration proxy for Maps.
     * The purpose of this proxy is to provide a way to bootstrap a concrete Map instance (e.g.
     * in this case, a HashMap) from a generic Map property type declaration.
     */
    static class MapIOCProxy extends HashMap implements IOCProxy {
        @Override
        public void initializeWithValue(Object value) {
            // This method won't be called in normal operation (it's only used for in-place values,
            // and if an object already has an in-place map instance then there's no need to
            // bootstrap a concrete instance).
            if( value instanceof Map ) {
                putAll( (Map)value );
            }
        }
        @Override
        public Object unwrapValue() {
            return this;
        }
    }

    static {
        // Register the List and Map configuration proxies.
        IOCProxyLookup.registerProxyClass( ListIOCProxy.class, "java.util.List");
        IOCProxyLookup.registerProxyClass( MapIOCProxy.class, "java.util.Map");
    }
}
