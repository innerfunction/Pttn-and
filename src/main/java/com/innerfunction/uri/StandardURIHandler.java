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
package com.innerfunction.uri;

import android.content.Context;
import android.util.Log;

import com.innerfunction.util.Assets;
import com.innerfunction.util.Files;
import com.innerfunction.util.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URISyntaxException;

/**
 * A class for handling internal URIs.
 * Supports resolution of relative URIs to absolute, and dereferencing of URIs to their
 * referred values.
 * TODO Is a separate URIHandler interface really necessary? If not then get rid of it and rename
 * TODO this class to URIHandler; otherwise consider adding formats and aliases properties to the
 * TODO interface.
 */
public class StandardURIHandler implements URIHandler {

    private static final String LogTag = "URIHandler";

    /** A map of handler instances by app context. */
    private static final Map<Context,StandardURIHandler> Instances = new HashMap<>();

    /** A map of the registered URI scheme handlers, keyed by scheme name. */
    private Map<String,URIScheme> schemeHandlers = new HashMap<>();
    /**
     * A map of URI scheme contexts.
     * Contains reference absolute URIs for different schemes.
     */
    private Map<String,CompoundURI> schemeContexts = new HashMap<>();
    /** The asset manager used by the resolver. */
    private Assets assets;
    /** A map of named URI formatters. */
    private Map<String,URIValueFormatter> formats;
    /** A map of URI aliases. */
    private Map<String,String> aliases;
    /** Constructor used for context singleton instances. */
    private StandardURIHandler(Context context) {
        this(context, new Assets(context));
    }

    /** Constructor used to build copies of handlers with modified scheme contexts. */
    private StandardURIHandler(StandardURIHandler handler, CompoundURI uri) {
        this.schemeHandlers = handler.schemeHandlers;
        this.assets = handler.assets;
        this.schemeContexts = Maps.extend( handler.schemeContexts, uri.getScheme(), uri );
        this.formats = handler.formats;
        this.aliases = handler.aliases;
    }

    /**
     * Create a resolver with the default scheme handlers.
     * The default schemes are:
     * <ul>
     *     <li><b>s:</b> The string scheme.</li>
     *     <li><b>a:</b> The alias scheme. This is a pseudo scheme without a specific handler,
     *     instead it is dereferenced by the URI handler.</li>
     *     <li><b>app:</b> For accessing app resources and assets.</li>
     *     <li><b>cache:</b> For accessing files in the app's cache location.</li>
     *     <li><b>local:</b> For accessing values in the app's local storage.</li>
     *     <li><b>repr:</b> For accessing non-default value representations.</li>
     * </ul>
     */
    public StandardURIHandler(Context context, Assets assets) {
        schemeHandlers.put("s",     new StringScheme( context ) );
        schemeHandlers.put("app",   new AnRBasedScheme( context, assets ) );
        schemeHandlers.put("cache", new FileBasedScheme( context, Files.getCacheDir( context )));
        schemeHandlers.put("local", new LocalScheme(context));
        schemeHandlers.put("repr",  new ReprScheme(context));
    }

    public Assets getAssets() {
        return assets;
    }

    public void setFormats(Map<String,URIValueFormatter> formats) {
        this.formats = formats;
    }

    public Map<String,URIValueFormatter> getFormats() {
        return formats;
    }

    public void setAliases(Map<String,String> aliases) {
        this.aliases = aliases;
    }

    public Map<String,String> getAliases() {
        return aliases;
    }

    /** Test if a URI scheme has a registered handler with this resolver. */
    @Override
    public boolean hasHandlerForURIScheme(String scheme) {
        return schemeHandlers.containsKey( scheme );
    }

    /** Register a new scheme handler. */
    @Override
    public void addHandlerForScheme(String scheme, URIScheme handler) {
        schemeHandlers.put( scheme, handler );
    }

    /** Return the set of registered URI scheme names. */
    @Override
    public List<String> getURISchemeNames() {
        return new ArrayList<>( schemeHandlers.keySet() );
    }

    /** Return the URI handler for the specified scheme. */
    @Override
    public URIScheme getHandlerForURIScheme(String scheme) {
        return schemeHandlers.get( scheme );
    }

    /**
     * Dereference a URI string.
     * @param uri An unparsed compound URI string. @see CompoundURI.
     *            If the string isn't a valid URI then an error is logged and null is returned.
     * @return The value or resource referenced by the URI.
     */
    @Override
    public Object dereference(String uri) {
        Object value = null;
        try {
            value = dereference( CompoundURI.parse( uri ) );
        }
        catch(URISyntaxException e) {
            Log.e( LogTag, String.format("Parsing '%s'", uri ), e );
        }
        return value;
    }

    /**
     * Dereference a compound URI.
     * Returns the resource or value referenced by the URI.
     */
    @Override
    public Object dereference(CompoundURI uri) {
        Object value = null;
        URIScheme handler = schemeHandlers.get( uri.getScheme() );
        if( handler != null ) {
            // Resolve parameter values.
            Map<String,CompoundURI> params = uri.getParameters();
            Map<String,Object> paramValues = new HashMap<>( params.size() );
            for( String name : params.keySet() ) {
                Object paramValue = dereference( params.get( name ) );
                if( paramValue != null ) {
                    paramValues.put( name, paramValue );
                }
            }
            // Try to ensure that the current URI is an absolute URI.
            CompoundURI referenceURI = schemeContexts.get( uri.getScheme() );
            if( referenceURI != null && handler instanceof RelativeURIScheme ) {
                uri = ((RelativeURIScheme)handler).resolveAgainst( uri, referenceURI );
            }
            // Ask the scheme handler to dereference the URI.
            value = handler.dereference( uri, paramValues );
        }
        else if( "a".equals( uri.getScheme() ) ) {
            // The a: scheme is a pseudo-scheme which is handled by the URI handler rather than a
            // specific scheme handler. Lookup a URI alias and dereference that.
            String aliasedURI = aliases.get( uri.getName() );
            value = dereference( aliasedURI );
        }
        else {
            Log.e( LogTag, String.format("Handler not found for scheme '%s'", uri.getScheme() ) );
        }
        // If the value result is a Resource instance then sets its uriHandler property to a copy
        // of this handler, but with a modified scheme context with the current scheme name mapped
        // to the resource's URI.
        if( value instanceof Resource ) {
            ((Resource)value).setURIHandler( modifySchemeContext( uri ) );
        }
        // If the URI specifies a formatter then apply it to the URI result.
        String format = uri.getFormat();
        if( format != null ) {
            URIValueFormatter formatter = formats.get( format );
            if( formatter != null ) {
                value = formatter.formatValue( value, uri );
            }
            else {
                String message = String.format("Formatter not found for name %s", format );
                throw new RuntimeException( message );
            }
        }
        // Return the result.
        return value;
    }

    /**
     * Return a copy of this handler with a modified scheme context.
     * The scheme context is modified by mapping the scheme name of the URI argument to the URI
     * argument.
     */
    @Override
    public URIHandler modifySchemeContext(CompoundURI uri) {
        return new StandardURIHandler( this, uri );
    }

    /**
     * Return a copy of this URI handler with a replacement scheme handler.
     */
    @Override
    public URIHandler replaceURIScheme(String schemeName, URIScheme scheme) {
        schemeHandlers.put( schemeName, scheme );
        return this;
    }

    /**
     * Return a singleton instance of this class for the specified context.
     */
    public static synchronized StandardURIHandler getInstance(Context context) {
        StandardURIHandler handler = Instances.get( context );
        if( handler == null ) {
            handler = new StandardURIHandler( context );
            Instances.put( context, handler );
        }
        return handler;
    }

}
