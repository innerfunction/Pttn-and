package com.innerfunction.pttn;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class responsible for configuring an object. Processes the object's configuration
 * by resolving property values and injecting them into the object.
 *
 * Created by juliangoacher on 15/04/16.
 */
public class ObjectConfigurer {

    static final String Tag = Object.class.getSimpleName();

    /** The object being configured. */
    private Object object;
    /** The object container. */
    private Container container;
    /** The object's property info. */
    private Map<String,Property> properties;
    /**
     * If the object being configured is a collection (i.e. a dictionary or array) then
     * this var contains the type hint for its members.
     */
    private Class collectionMemberTypeHint;
    /** The key path to the object's configuration. */
    private String keyPath;
    /** A flag indicating whether the object being configured is a collection. */
    private boolean isCollection;

    /**
     * Initialize the configurer with the object to configure.
     * @param object    The object being configured.
     * @param container The object container.
     * @param keyPath   The key-path to the object's configuration.
     */
    public ObjectConfigurer(Object object, Container container, String keyPath) {
        this.object = object;
        this.container = container;
        this.properties = Property.getPropertiesForObject( object );
        this.keyPath = keyPath;
        this.isCollection = (object instanceof List) || (object instanceof Map);
    }

    /**
     * Initialize a container configurer.
     * A container is considered a collection (i.e. of named objects) with a default collection
     * member type of 'Object'.
     * @param container The container to be configured.
     */
    public ObjectConfigurer(Container container) {
        this( container, container, "");
        this.collectionMemberTypeHint = Object.class;
        this.isCollection = true;
    }

    /** Return the object being configured. */
    public Object getObject() {
        return object;
    }

    /** Perform the object configuration. */
    public void configureWith(Configuration configuration) {

        // Lists needs special handling when being configured, because list items won't necessarily
        // be presented to the configurer in list order, which may then cause problems when the
        // configurer attempts to add items out of order and has to insert null-value placeholders
        // into the list to try and keep the intended order. The solution used here is to
        // temporarily swap the list being configured with a ListBackedMap instance, which can
        // handle items being inserted in random order,and which also handles mapping between
        // string property names and integer indexes.
        List listObject = null;
        if( object instanceof List ) {
            listObject = (List)object;
            object = new ListBackedMap( new ArrayList<>() );
        }

        // Start the object configuration.
        if( object instanceof IOCContainerAware ) {
            ((IOCContainerAware)object).beforeIOCConfigure( configuration );
        }
        List<String> valueNames = configuration.getValueNames();
        for( String name : valueNames ) {
            String propName = normalizePropertyName( name );
            if( propName == null ) {
                continue;
            }
            configureProperty( propName, configuration );
        }

        // If configuring a list then swap the target list back and add the newly configured items
        // into it.
        if( listObject != null ) {
            // The following inserts values into the target list, skipping any null-values in the
            // configuration result. If the target list is already populated with items then the
            // new values will be interpolated in over the existing values; otherwise the new
            // items are simply appended to the end of the target list.
            List items = ((ListBackedMap)object).getList();
            for( int i = 0; i < items.size(); i++ ) {
                Object item = items.get( i );
                if( item != null ) {
                    if( i < listObject.size() ) {
                        listObject.set( i, item );
                    }
                    else {
                        listObject.add( item );
                    }
                }
            }
            object = listObject;
        }

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
     * Configure a single property from the specified object configuration.
     * @param propName      The name of the property to configure.
     * @param configuration The object configuration; should contain the property configuration.
     * @return The value the property was configured with.
     */
    public Object configureProperty(String propName, Configuration configuration) {
        Object value = null;
        Property property = getPropertyInfo( propName );
        if( property != null ) {

            if( !property.isAnyType() ) {
                if( property.isAssignableFrom( Boolean.class ) ) {
                    value = configuration.getValueAsBoolean( propName );
                }
                else if( property.isAssignableFrom( Number.class ) ) {
                    Number number = configuration.getValueAsNumber( propName );
                    if( property.isType( Integer.class ) ) {
                        value = number.intValue();
                    }
                    else if( property.isType( Float.class ) ) {
                        value = number.floatValue();
                    }
                    else if( property.isType( Double.class ) ) {
                        value = number.doubleValue();
                    }
                    else {
                        value = number;
                    }
                }
                else if( property.isAssignableFrom( String.class ) ) {
                    value = configuration.getValueAsString( propName );
                }
                else if( property.isAssignableFrom( Date.class ) ) {
                    value = configuration.getValueAsDate( propName );
                }
                else if( property.isAssignableFrom( Drawable.class ) ) {
                    value = configuration.getValueAsImage( propName );
                }
                else if( property.isAssignableFrom( Color.class ) ) {
                    value = configuration.getValueAsColor( propName );
                }
                else if( property.isAssignableFrom( Configuration.class ) ) {
                    value = configuration.getValueAsConfiguration( propName );
                }
            }

            if( value == null ) {
                Configuration.Maybe maybeConfig = configuration.getValueAsMaybeConfiguration( propName );
                Configuration valueConfig = maybeConfig.getConfiguration();
                if( valueConfig != null ) {

                    // Flag indicating whether the resolved value should be configured in turn.
                    // Default is yes, but some values will skip the configuration step.
                    boolean configureValue = true;

                    // If we have an item configuration with an instantiation hint then try using
                    // to build an object. Note that instantiation hints take priority over
                    // in-place values, i.e. a configuration with an instantiation hint will force
                    // build a new value even if there is already an in-place value.
                    Object factory = valueConfig.getValue( "*factory" );
                    if( factory != null ) {
                        // The configuration specifies an object factory, so resolve the factory
                        // object and attempt using it to instantiate the object.
                        if( factory instanceof IOCObjectFactory ) {
                            value = ((IOCObjectFactory)factory).buildObject( valueConfig, container, propName );
                            container.doPostInstantiation( value );
                            container.doPostConfiguration( value );
                        }
                        else {
                            Log.w( Tag, String.format( "Invalid *factory class %s referenced at %s",
                                factory.getClass(), getKeyPath( propName ) ) );
                        }
                        // NOTE that factory instantiated objects do not go through the standard dependency-injection
                        // configuration process (in the following 'else' block).
                    }
                    else {
                        // Try instantiating object from type or class info.
                        value = container.instantiateObjectWithConfiguration( valueConfig, propName );
                        // Unable to instantiate a value, check for an in-place value.
                        if( value == null ) {
                            value = property.get( object );
                        }
                        // If value is a collection then we need a mutable copy before progressing.
                        if( value != null ) {
                            // Apply configuration proxy wrapper, if any defined.
                            value = IOCProxyLookup.applyConfigurationProxyWrapper( value );
                        }

                        // If we get to this point and value is still nil then the following things are
                        // true:
                        // a. The value config doesn't contain any instantiation hint.
                        // b. The value config data is a collection (JSON object or list)
                        // At this point we now decide whether to use the plain JSON data as the
                        // property value, based on the following:
                        // * If the property being configured is a collection (List or Map);
                        // * And the property is not a parameterized type (i.e. no member type
                        //   info is provided via generics)
                        // * Then use the plain data value.
                        // Note that this is different from the iOS implementation, which relies
                        // on the IOCTypeInspectable protocol being used to declare bare-data
                        // properties.
                        boolean isListProp = property.isAssignableFrom( List.class );
                        boolean isMapProp = !isListProp && property.isAssignableFrom( Map.class );
                        if( value == null && (isListProp || isMapProp) ) {
                            Type[] memberTypeInfo = property.getGenericParameterTypeInfo();
                            if( memberTypeInfo == null ) {
                                // No type information declared for the collection's members.
                                value = maybeConfig.getData();
                                // The value should be treated as plain data (i.e. contains no
                                // configurables) so skip the configuration step.
                                configureValue = false;
                            }
                        }

                        // Couldn't find an in-place value, so try instantiating a value.
                        if( value == null ) {
                            if( isListProp ) {
                                value = new ArrayList();
                            }
                            else if( isMapProp ) {
                                value = new HashMap();
                            }
                            else if( !property.isAnyType() ) {
                                // Try using the property info as a type hint. (But only if a
                                // specific type).
                                String className = property.getTypeClassName();
                                try {
                                    value = container.newInstanceForClassNameAndConfiguration( className, valueConfig );
                                }
                                catch(Exception e) {
                                    Log.e( Tag, String.format( "Error creating new instance of inferred type %s", className ), e );
                                }
                            }
                        }

                        // If we have a value now and it should be configured then create a
                        // configurer for it.
                        if( value != null && configureValue ) {
                            ObjectConfigurer configurer = new ObjectConfigurer( value, container, getKeyPath( propName ) );
                            // If dealing with a collection value then add type info for its
                            // members.
                            if( value instanceof List ) {
                                Type[] memberTypeInfo = property.getGenericParameterTypeInfo();
                                if( memberTypeInfo != null && memberTypeInfo.length > 0 && memberTypeInfo[0] instanceof Class ) {
                                    configurer.collectionMemberTypeHint = (Class)memberTypeInfo[0];
                                }
                            }
                            else if( value instanceof Map ) {
                                Type[] memberTypeInfo = property.getGenericParameterTypeInfo();
                                if( memberTypeInfo != null && memberTypeInfo.length > 1 && memberTypeInfo[1] instanceof Class ) {
                                    configurer.collectionMemberTypeHint = (Class)memberTypeInfo[1];
                                }
                            }
                            // Configure the value.
                            configurer.configureWith( valueConfig );
                        }
                    }
                }
                // If still no value by this stage then try using whatever underlying value is in
                // the maybe config.
                if( value == null ) {
                    value = maybeConfig.getBare();
                }
            }
        }
        // If there is a value by this stage then inject into the object.
        if( value != null ) {
            value = injectValueIntoProperty( value, propName );
        }
        return value;
    }

    public Object injectValueIntoProperty(Object value, String propName) {
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
            Property property = getPropertyInfo( propName );
            if( property.isAssignableFrom( value.getClass() ) ) {
                // Standard object property reference.
                property.set( object, value );
            }
        }
        return value;
    }

    /**
     * Normalize a property name by removing any *ios- prefix. Returns null for reserved names
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
     * Get type info for a named property of the object being configured.
     */
    private Property getPropertyInfo(String propName) {
        Property property = properties.get( propName );
        if( property == null && isCollection ) {
            // Create a property subclass suitable for reading/writing to Map entries.
            // TODO Should this code be move to somewhere else, e.g. Property?
            // TODO Note that this arrangement supports a dual-mode when accessing properties of
            // TODO a collection instance - (1) actual properties of the object, with getter/setter
            // TODO methods; and (2) properties as named entries of the collection object.
            property = new Property( propName, collectionMemberTypeHint ) {
                public Type[] getGenericParameterTypeInfo() {
                    return null;
                }
                public boolean set(Object object, Object value) {
                    ((Map)object).put( getName(), value );
                    return true;
                }
                public Object get(Object object) {
                    return ((Map)object).get( getName() );
                }
            };
        }
        return property;
    }

    private String getKeyPath(String name) {
        return String.format("%s.%s", keyPath, name );
    }
}
