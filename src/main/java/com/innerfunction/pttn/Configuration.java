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
package com.innerfunction.pttn;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.innerfunction.uri.Resource;
import com.innerfunction.uri.URIHandler;
import com.innerfunction.util.KeyPath;
import com.innerfunction.util.Maps;
import com.innerfunction.util.StringTemplate;
import com.innerfunction.util.TypeConversions;

/**
 * A class used to parse and access component configurations.
 */
public class Configuration {

    static final String Tag = Configuration.class.getSimpleName();

    /**
     * An object returned by the Configuration.getValueAsMaybeConfiguration() method.
     * The class is used to represent configuration values that might themselves be used as
     * configurations (the point being that it's not known at the point when the method is
     * called whether they are or not). Objects of this type are used by the ObjectConfigurer
     * class as an optimization representing intermediate configuration states whilst an
     * object graph is being built.
     */
    public static class Maybe {

        /** The configuration, if any. */
        private Configuration configuration;
        /** The underlying configuration data, i.e. the value read from the configuration JSON. */
        private Object data;
        /** The underlying data's bare representation. */
        private Object bare;

        Maybe(Object configuration, Object bare) {
            if( configuration instanceof Configuration ) {
                this.configuration = ((Configuration)configuration).normalize();
            }
            this.bare = bare;
            if( bare instanceof Resource ) {
                this.data = ((Resource)bare).asJSONData();
            }
            else {
                this.data = bare;
            }
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public Object getData() {
            return data;
        }

        public Object getBare() {
            if( bare instanceof ListBackedMap ) {
                return ((ListBackedMap)bare).getList();
            }
            return bare;
        }
    }

    /** The configuration data. */
    private Map<String,Object> data;
    /** The root configuration. Used to resolve # value references. */
    private Configuration root = this;
    /** A URI handler for dereferencing URIs. */
    private URIHandler uriHandler;
    /**
     * The configuration's data context.
     * Used as the data context for templated values. A configuration supports two types of templated
     * values:
     * <ul>
     *     <li>String template values, indicated by property values prefixed with ?;</li>
     *     <li>Configuration template values, indicated by property values prefixed with $.</li>
     * </ul>
     */
    private Map<String,Object> context;
    /** The app context. */
    private Context androidContext;
    /** Android resources. */
    private Resources r;
    /** Functions for converting between types. */
    private TypeConversions conversions;

    /**
     * Root configuration constructor.
     * This method is used from e.g. the app container to create the root configuration. All
     * subsequent configurations are instantiated using the root, or one of its derivatives, as
     * their parent configuration.
     * @param data              The configuration data.
     * @param uriHandler        An internal URI handler.
     * @param androidContext    The app context.
     */
    @SuppressWarnings("unchecked")
    public Configuration(Object data, URIHandler uriHandler, Context androidContext) {
        this.uriHandler = uriHandler;
        this.conversions = TypeConversions.instanceForContext( androidContext );
        this.androidContext = androidContext;
        this.context = new HashMap<>();
        setData( data );
        initialize();
    }

    /**
     * General configuration constructor.
     * Used to create a configuration using data and a parent configuration.
     * @param data      The configuration data.
     * @param parent    The parent configuration.
     */
    @SuppressWarnings("unchecked")
    public Configuration(Object data, Configuration parent) {
        this.uriHandler = parent.uriHandler;
        this.conversions = parent.conversions;
        this.androidContext = parent.androidContext;
        this.root = parent;
        this.context = parent.context;
        setData( data );
        initialize();
    }

    /**
     * Create a configuration using the specified data.
     * The configuration parent is an empty configuration. Use this constructor for configuration
     * templates.
     * @param data      The configuration data.
     */
    public Configuration(Object data) {
        this( data, new Configuration() );
    }

    /**
     * Create a configuration using data provided by a resource.
     * @param resource  A resource containing configuration data.
     * @param parent    The parent resource.
     */
    @SuppressWarnings("unchecked")
    protected Configuration(Resource resource, Configuration parent) {
        this( resource.asJSONData(), parent );
    }

    /**
     * Create an empty configuration.
     * This constructor is only used internally for creating a base configuration which is about
     * to be extended with additional data. An empty configuration lacks certain key properties
     * - e.g. a URI handler - so isn't fully functional.
     */
    private Configuration() {
        this.data = new HashMap<>();
        this.context = new HashMap<>();
    }

    /**
     * Create a new configuration by merging in properties from one config over another.
     * The two configurations are merged by performing a top-level copy of the properties in one
     * over the properties in the other.
     * @param config    The base  configuration.
     * @param mixin     The mixin configuration; its properties are copied over the base config.
     * @param parent    A configuration to use as the parent (provides URI handler etc.). Typically
     *                  either the config or mixin argument.
     */
    private Configuration(Configuration config, Configuration mixin, Configuration parent) {
        this.uriHandler = parent.uriHandler;
        this.conversions = parent.conversions;
        this.androidContext = parent.androidContext;
        this.root = parent.root;
        this.data = Maps.mixin( config.data, mixin.data );
        this.context = Maps.mixin( config.context, mixin.context );
        initialize();
    }

    /**
     * Setup the configuration's initial state.
     * Copies any configuration parameter values from the config data to the context.
     */
    private void initialize() {
        this.r = androidContext.getResources();
        // Search the configuration data for any parameter values, and move any found to a separate map.
        Map<String,Object> params = new HashMap<>();
        for( String key : data.keySet() ) {
            if( key.startsWith("$") ) {
                params.put( key, data.get( key ) );
                data.remove( key );
            }
        }
        // Add param values to the context.
        if( params.size() > 0 ) {
            context.putAll( params );
        }
    }

    /** Set the configuration data. */
    public void setData(Object data) {
        if( data instanceof String ) {
            data = conversions.asJSONData( data );
        }
        if( data instanceof List ) {
            data = new ListBackedMap( (List)data );
        }
        if( data instanceof Map ) {
            this.data = (Map<String,Object>)data;
        }
    }

    /** Get the config data. */
    public Map<String,Object> getData() {
        return data;
    }

    /**
     * Get the configuration's URI handler.
     * This is useful as the handler contains the URI context used to resolve the configuration
     * data.
     */
    public URIHandler getURIHandler() {
        return uriHandler;
    }

    /** Set the configuration's URI handler. */
    public void setURIHandler(URIHandler uriHandler) {
        this.uriHandler = uriHandler;
    }

    /** Set the configuration's context data. */
    public void setContext(Map<String,Object> context) {
        this.context = context;
    }

    /**
     * Resolve a configuration value.
     * Recognizes value prefixes and performs the appropriate operation. Converts the final value to
     * the required representation.
     * @param keyPath           A path of keys separated by full stops.
     * @param representation    The name of the required value representation.
     * @return Returns the value at key path, with the appropriate conversion applied; or returns
     * null if no value is found.
     */
    public Object getValueAs(String keyPath, final String representation) {
        Object value = KeyPath.resolve( keyPath, data, new KeyPath.Modifier() {
            /**
             * Modify property owners before performing a lookup.
             * Convert intermediate Resource values to their data representation.
             */
            public Object modifyObject(String key, Object object) {
                if (object instanceof Resource) {
                    return ((Resource) object).asJSONData();
                }
                return object;
            }
            /**
             * Modify property values by examining value prefixes.
             */
            public Object modifyValue(String key, Object value) {
                if( value instanceof String ) {
                    String valueStr = (String)value;
                    char prefix = 0x0;
                    if( valueStr.length() > 1 ) {
                        prefix = valueStr.charAt(0);
                    }
                    // First, attempt resolving any context references. If these in turn resolve
                    // to a $ or # prefixed value, then they will be resolved in the following
                    // code.
                    if( prefix == '$' ) {
                        value = context.get( valueStr );
                        // If the resolved value is also a string then continue to the following
                        // modifier prefixes; otherwise return the value.
                        if( value instanceof String ) {
                            valueStr = (String)value;
                            if( valueStr.length() > 1 ) {
                                prefix = valueStr.charAt(0);
                            }
                            else {
                                prefix = 0x00; // Null prefix.
                            };
                        }
                        else {
                            valueStr = null;
                            prefix = 0x00;
                        }
                    }
                    // Evaluate any string beginning with ? as a string template.
                    if( prefix == '?' ) {
                        valueStr = StringTemplate.render( valueStr, context );
                        // Check for a new prefix.
                        if( valueStr.length() > 1 ) {
                            prefix = valueStr.charAt( 0 );
                        }
                        else {
                            prefix = 0x00;
                        }
                    }
                    // String values beginning with @ are internal URI references, so dereference the URI.
                    if( prefix == '@' ) {
                        String uri = valueStr.substring( 1 );
                        value = uriHandler.dereference( uri );
                    }
                    // Any string values starting with a '#' are potential path references to
                    // other properties on the root configuration. Attempt to resolve these; if
                    // they don't resolve the return the original value.
                    else if( prefix == '#' ) {
                        value = root.getValueAs( valueStr.substring( 1 ), representation );
                        if( value == null ) {
                            value = valueStr;
                        }
                    }
                    // A backtick prefix is used to escape any other prefixes; simply remove
                    // from the value and return the remainder.
                    else if( prefix == '`' ) {
                        value = valueStr.substring( 1 );
                    }
                    // Else no recognized prefix, so return the string value (which note may
                    // have been generated from a string template by this stage).
                    else if( valueStr != null ) {
                        value = valueStr;
                    }
                }
                return value;
            }
        });
        // Perform type conversions according to the requested representation.
        // * 'bare' representations don't need to be converted.
        // * 'configuration' reprs can be constructed from dictionary instances or resources.
        // * Resource instances can be used to perform the requested representation conversion.
        // * Otherwise use the type conversions to resolve the representation.
        if( !"bare".equals( representation ) ) {
            if( "configuration".equals( representation ) || "maybe-configuration".equals( representation ) ) {

                Object bareValue = value;

                if( !(value instanceof Configuration) ) {
                    // If value is a list then convert to a list backed map.
                    if( value instanceof List ) {
                        value = new ListBackedMap( (List)value );
                    }
                    // If value isn't already a configuration, but is a map then construct a new
                    // config using its values.
                    if( value instanceof Map && androidContext != null ) {
                        value = new Configuration( value, this );
                    }
                    // Else if value is a resource, then construct a new config using the resource...
                    else if( value instanceof Resource ) {
                        value = new Configuration( (Resource)value, this );
                    }
                    // Else the value can't be resolved to a configuration, so return null.
                    else {
                        value = null;
                    }
                }

                // Wrap result in a maybe if that is what was requested.
                if( "maybe-configuration".equals( representation ) ) {
                    value = new Maybe( value, bareValue );
                }
            }
            else if( value instanceof Resource ) {
                value = ((Resource)value).asRepresentation( representation );
            }
            else if( !"json".equals( representation ) ) {
                value = conversions.asRepresentation( value, representation );
            }
        }
        return value;
    }

    /** Test if a non-null configuration value exists at the specified key path. */
    public boolean hasValue(String keyPath) {
        return getValueAs( keyPath, "bare" ) != null;
    }

    /** Get a configuration value as a string. */
    public String getValueAsString(String keyPath) {
        return getValueAsString( keyPath, null );
    }

    /** Get a configuration value as a string. */
    public String getValueAsString(String keyPath, String defaultValue) {
        String value = (String)getValueAs( keyPath, "string");
        return value == null ? defaultValue : value;
    }

    /** Get a configuration value as a localized string. */
    public String getValueAsLocalizedString(String keyPath) {
        String result = null;
        String value = getValueAsString( keyPath );
        if( value != null ) {
            String packageName = androidContext.getPackageName();
            int rid = r.getIdentifier( value, "string", packageName );
            if( rid > 0 ) {
                result = r.getString( rid );
            }
        }
        return result;
    }

    /** Get a configuration value as a number. */
    public Number getValueAsNumber(String keyPath) {
        return getValueAsNumber( keyPath, null );
    }

    /** Get a configuration value as a number. */
    public Number getValueAsNumber(String keyPath, Number defaultValue) {
        Number value = (Number)getValueAs( keyPath, "number");
        return value == null ? defaultValue : value;
    }

    /** Get a configuration value as a boolean. */
    public Boolean getValueAsBoolean(String keyPath) {
        return getValueAsBoolean( keyPath, Boolean.FALSE );
    }

    /** Get a configuration value as a boolean. */
    public Boolean getValueAsBoolean(String keyPath, Boolean defaultValue) {
        Boolean value = (Boolean)getValueAs( keyPath, "boolean");
        return value == null ? defaultValue : value;
    }

    /** Get a configuration value as a date. */
    public Date getValueAsDate(String keyPath) {
        return getValueAsDate( keyPath, null );
    }

    /** Get a configuration value as a date. */
    public Date getValueAsDate(String keyPath, Date defaultValue) {
        Date value = (Date)getValueAs( keyPath, "date");
        return value == null ? defaultValue : value;
    }

    /** Get a configuration value as an external URL. */
    public URI getValueAsURL(String keyPath) {
        return (URI)getValueAs( keyPath, "url");
    }

    /** Get a configuration value as a byte array. */
    public byte[] getValueAsData(String keyPath) {
        return (byte[])getValueAs( keyPath, "data");
    }

    /** Get a configuration value as an image drawable. */
    public Drawable getValueAsImage(String keyPath) {
        return (Drawable)getValueAs( keyPath, "image");
    }

    /** Get a configuration value as a colour. */
    public int getValueAsColor(String keyPath) {
        return getValueAsColor( keyPath, "#000000" );
    }

    /** Get a configuration value as a colour. */
    public int getValueAsColor(String keyPath, String defaultValue) {
        String value = getValueAsString( keyPath, defaultValue );
        return conversions.asColor( value );
    }

    /** Get a configuration value as an internal resource. */
    public Resource getValueAsResource(String keyPath) {
        return (Resource)getValueAs( keyPath, "resource");
    }

    /** Get a configuration value in its bare (i.e. default) representation, with no conversions applied. */
    public Object getValue(String keyPath) {
        return getValueAs( keyPath, "bare");
    }

    /** Return a list of the top-level value names in the configuration data. */
    public List<String> getValueNames() {
        return new ArrayList<>( data.keySet() );
    }

    /**
     * An enumeration of value types.
     * The enumerated types correspond to the standard JSON types.
     */
    public enum ValueType { Object, List, String, Number, Boolean, Undefined };

    /** Get the type of a configuration value. */
    public ValueType getValueType(String keyPath) {
        Object value = getValueAs( keyPath, "json");
        if( value == null )             return ValueType.Undefined;
        if( value instanceof Boolean )  return ValueType.Boolean;
        if( value instanceof Number )   return ValueType.Number;
        if( value instanceof String )   return ValueType.String;
        if( value instanceof List )     return ValueType.List;
        return ValueType.Object;
    }

    /** Get a configuration value as a configuration object. */
    public Configuration getValueAsConfiguration(String keyPath) {
        Configuration value = (Configuration)getValueAs( keyPath, "configuration");
        if( value != null ) {
            value = value.normalize();
        }
        return value;
    }

    /** Get a configuration value as a configuration object. */
    public Configuration getValueAsConfiguration(String keyPath, Configuration defaultValue) {
        Configuration result = getValueAsConfiguration( keyPath );
        return result == null ? defaultValue : result;
    }

    /** Get a value as maybe a configuration. */
    public Maybe getValueAsMaybeConfiguration(String keyPath) {
        return (Maybe)getValueAs( keyPath, "maybe-configuration");
    }

    /**
     * Get a configuration value as a list of configuration objects.
     * The bare value must be a List value type. A configuration object is instantiated for each
     * item on the list, using the item as the configuration data.
     * @param keyPath
     * @return
     */
    @SuppressLint("DefaultLocale")
    public List<Configuration> getValueAsConfigurationList(String keyPath) {
        List<Configuration> result = new ArrayList<>();
        Object value = getValue( keyPath );
        if( !(value instanceof List) ) {
            value = getValueAs( keyPath, "json");
        }
        if( value instanceof List ) {
            @SuppressWarnings("rawtypes")
            List values = (List)value;
            for( int i = 0; i < values.size(); i++ ) {
                String itemPath = String.format("%s.%d", keyPath, i );
                Configuration item = getValueAsConfiguration( itemPath );
                result.add( item );
            }
        }
        return result;
    }

    /**
     * Get a configuration value as a map of configuration objects.
     * The bare value must be a Map value type. A configuration object is instantiated for each
     * item on the map, using the item value as the configuration data.
     * @param keyPath
     * @return
     */
    public Map<String,Configuration> getValueAsConfigurationMap(String keyPath) {
        Map<String,Configuration> result = new HashMap<>();
        Object value = getValue( keyPath );
        if( value instanceof Map ) {
            @SuppressWarnings("rawtypes")
            Map values = (Map)value;
            for( Object key : values.keySet() ) {
                String itemPath = String.format("%s.%s", keyPath, key );
                Configuration item = getValueAsConfiguration( itemPath );
                result.put( key.toString(), item );
            }
        }
        return result;
    }

    /**
     * Create a new configuration by merging properties from another configuration over the current
     * configuration.
     * The merge works by performing a top-level copy of properties from the argument to the current
     * object. This means that names in the argument will overwrite any properties with the same
     * name in the current configuration.
     * The root, context and uriHandler properties of the current configuration are copied to the
     * result.
     */
    public Configuration mixinConfiguration(Configuration otherConfig) {
        return new Configuration( this, otherConfig, this );
    }

    /**
     * Create a new configuration by merging the values of the current configuration over the values
     * of another configuration.
     * This method is similar to the mixinConfiguration method except that value precedence is in
     * the reverse order (i.e. the current configuration's values take precedence over the other
     * configs.
     * The root, context and uriHandler properties of the current configuration are copied to the
     * result.
     */
    public Configuration mixoverConfiguration(Configuration otherConfig) {
        return new Configuration( otherConfig, this, this );
    }

    /**
     * Extend this configuration with the specified set of parameters.
     * As well as being added to the current configuration (see Configuration.extend), the
     * parameters are added to the current template scope with a $ prefix before each parameter
     * name. This allows parameters to then be used in two ways from within a configuration:
     * - As direct parameter references, by using a value with the $ prefix, e.g. "$param1";
     * - As parameter references from within template strings, e.g. "view:X+id@{$param1}"
     */
    public Configuration extendWithParameters(Map<String,Object> params) {
        Configuration result = this;
        if( params.size() > 0 ) {
            result = new Configuration( data, this );
            result.context = new HashMap<>( result.context );
            for( String name : params.keySet() ) {
                result.context.put("$"+name, params.get( name ) );
            }
        }
        return result;
    }

    /**
     * Flatten the configuration by merging "*config", "*mixin" and "*mixins" properties.
     */
    public Configuration flatten() {
        Configuration result = this;
        Configuration mixin = getValueAsConfiguration( "*config" );
        if( mixin != null ) {
            result = mixinConfiguration( mixin );
        }
        mixin = getValueAsConfiguration("*mixin");
        if( mixin != null ) {
            result = mixinConfiguration( mixin );
        }
        List<Configuration> mixins = getValueAsConfigurationList("*mixins");
        if( mixins != null ) {
            for( Configuration mxn : mixins ) {
                result = mixinConfiguration( mxn );
            }
        }
        return result;
    }

    /** Normalize this configuration by flattening and resolving configuration extensions. */
    public Configuration normalize() {
        // Build the extension hierarchy.
        List<Configuration> hierarchy = new ArrayList<>();
        Configuration current = flatten();
        hierarchy.add( current );
        while( (current = current.getValueAsConfiguration("*extends")) != null ) {
            current = current.flatten();
            if( hierarchy.contains( current ) ) {
                // Extension loop detected, stop building the hierarchy.
                break;
            }
            hierarchy.add( current );
        }
        // Build a single unified configuration from the hierarchy of configs.
        Configuration result = new Configuration(); // Start with an empty config.
        // Process the hierarchy in reverse order (i.e. from most distant ancestor to current config).
        Collections.reverse( hierarchy );
        for( Configuration config : hierarchy ) {
            result = result.mixinConfiguration( config );
        }
        result.root = root;
        result.uriHandler = uriHandler;
        return result;
    }

    /** Return a copy of the current configuration with the specified top-level keys removed. */
    public Configuration configurationWithKeysExcluded(String... keys) {
        Map<String,Object> data = new HashMap<>();
        // Copy non-excluded keys to the new configuration's data.
        for( String key : this.data.keySet() ) {
            for( String excludedKey : keys ) {
                if( key.equals( excludedKey ) ) {
                    continue;
                }
            }
            data.put( key, this.data.get( key ) );
        }
        // Create and return the new configuration.
        return new Configuration( data, this );
    }

    /** Modify this configuration with a set of new values. */
    /*
    public void modify(Map<String,Object> data) {
        // Create a copy of the config's data and then add the new data.
        this.data = new HashMap<String,Object>( this.data );
        this.data.putAll( data );
    }
*/
    /** Modify a single value in this configuration's data. */
    /*
    public void modify(String name, Object value) {
        modify( Maps.mapWithEntry( name, value ) );
    }
*/
    @Override
    public int hashCode() {
        return data.hashCode() ^ context.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if( !(obj instanceof Configuration) ) {
            return false;
        }
        Configuration config = (Configuration)obj;
        return data.equals( config.data ) && context.equals( config.context );
    }

    @Override
    public String toString() {
        return String.format( "data: %s context: %s", data, context );
    }
}
