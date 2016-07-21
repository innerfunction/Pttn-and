package com.innerfunction.pttn;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
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
    }

    /** Perform the container configuration. */
    public void configureWith(Configuration configuration) {
        Properties properties = new ContainerProperties( container );
        configure( container, properties, configuration, "" );
    }

    /**
     * Configure an object.
     * @param object        The object to configure.
     * @param properties    Information about properties of the object.
     * @param configuration The object configuration.
     * @param kpPrefix      Key path prefix, i.e. the key path within the container to the object
     *                      being configured. Used for logging purposes.
     */
    public void configure(Object object, Properties properties, Configuration configuration, String kpPrefix) {
        // Start the object configuration.
        if( object instanceof IOCContainerAware ) {
            ((IOCContainerAware)object).beforeIOCConfigure( configuration );
        }
        // Configure the object.
        if( object instanceof Configurable ) {
            ((Configurable)object).configure( configuration, container );
        }
        else for( String name : configuration.getValueNames() ) {
            String propName = normalizePropertyName( name ); // Check for reserved names.
            if( propName != null ) {
                // Build a property value from the configuration.
                Object value = buildPropertyValue( propName, properties, configuration, kpPrefix );
                // If property value then inject into the object property.
                if( value != null ) {
                    injectPropertyValue( object, propName, properties, value );
                }
                configuredPropertyCount++;
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
     * @param propName      The name of the property being built.
     * @param properties    The set of properties of the object being configured.
     * @param configuration The object configuration; should contain a configuration for the named
     *                      property.
     * @param kpPrefix      The key path of the object being configured.
     * @return The property value built from the configuration, or null if no value can be resovled.
     */
    public Object buildPropertyValue(String propName, Properties properties, Configuration configuration, String kpPrefix) {
        Object value = null;
        Class<?> propType = properties.getPropertyType( propName );
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
                value = configuration.getValue( propName );
            }
        }
        // If value is still null then the property is not a primitive or JSON data type. Try to
        // construct a new value from the supplied configuration.
        if( value == null ) {
            // Try reading the property configuration.
            Configuration valueConfig = configuration.getValueAsConfiguration( propName );
            if( valueConfig != null ) {
                // Try asking the container to build a new object using the configuration. This
                // will only work if the configuration contains an instantiation hint (e.g. *type,
                // *factory etc.) and will return a fully-configured non-null object if successful.
                value = container.buildObject( valueConfig, getKeyPath( kpPrefix, propName ) );
                if( value == null ) {
                    // Couldn't build a value, so see if the object already has a value in-place.
                    value = properties.getPropertyValue( propName );
                    if( value != null ) {
                        // Apply configuration proxy wrapper, if any defined.
                        value = IOCProxyLookup.applyConfigurationProxyWrapper( value );
                    }
                    else if( propType != Object.class ) {
                        // No in-place value, so try inferring an value type from the property
                        // information, and then try to instantiate the type as the new value.
                        String className = propType.getCanonicalName();
                        try {
                            value = container.newInstanceForClassNameAndConfiguration( className, valueConfig );
                        }
                        catch(Exception e) {
                            Log.e( Tag, String.format("Error creating new instance of inferred type %s", className ), e );
                        }
                    }
                    // Test if we now have a value which is assignable to the property.
                    if( value != null && propType.isAssignableFrom( value.getClass() ) ) {
                        // So not configure the value. First gather information on the value's
                        // properties.
                        Properties valueProps;
                        if( value instanceof Map ) {
                            // Maps are configured the same as object instances, but properties are
                            // mapped to map entries instead of properties of the map class.
                            // Note that by this point, lists are presented as maps (see the
                            // ListIOCProxy class below).
                            Class<?> inferredType = properties.getMapPropertyValueTypeParameter( propName );
                            valueProps = new MapProperties( (Map<String,Object>)value, inferredType );
                        }
                        else {
                            valueProps = new ObjectProperties( value );
                        }
                        // Configure the value.
                        configure( value, valueProps, valueConfig, getKeyPath( kpPrefix, propName ) );
                    }
                }
            }
        }
        return value;
    }

    /**
     * Inject a value into an object property.
     * @param object        The property owner.
     * @param propName      The name of the property being configured.
     * @param properties    Information about the properties of the object being configured.
     * @param value         The value to inject.
     * @return Returns the value injected into the object property.
     */
    public Object injectPropertyValue(Object object, String propName, Properties properties, Object value) {
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
            pending.setConfigurer( this );
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
     * Normalize a property name by removing any *and- prefix. Returns null for reserved names
     * (e.g. *type etc.)
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
        return prefix.length() > 0 ? prefix+"."+name : name;
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
    interface Properties<T> {
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
        Map<String,T> map;
        /**
         * The type to infer for each of the map's properties.
         * This information is inferred from the map's generic type parameters.
         */
        Class<?> inferredMemberType;

        MapProperties(Map<String,T> map, Class<?> inferredMemberType) {
            this.map = map;
            this.inferredMemberType = inferredMemberType;
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
            return value != null ? value : container.getNamed( name );
        }
        /*
        @Override
        public boolean setPropertyValue(String name, Object value) {
            if( !super.setPropertyValue( name, value ) ) {
                container.setNamed( name, value );
            }
            return true;
        }
        */
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

    static {
        // Register the List configuration proxy.
        IOCProxyLookup.registerProxyClass( ListIOCProxy.class, "java.util.List");
    }
}
