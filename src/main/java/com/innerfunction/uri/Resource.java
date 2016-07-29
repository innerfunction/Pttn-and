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
import android.graphics.drawable.Drawable;

import java.net.URI;

import com.innerfunction.util.TypeConversions;

/**
 * Class representing a resource referenced by an internal compound URI.
 */
public class Resource {

    private static final String LogTag = Resource.class.getSimpleName();

    /** The resource's data data, i.e. the value referenced by the resource's URI. */
    private Object data;
    /** The URI used to reference the resource. */
    protected CompoundURI uri;
    /**
     * The URI handler used to resolve this resource.
     * If the resource has been resolved using a URI in a scheme that supports relative URI
     * references, then this URI handler's scheme context will have this resource's absolute
     * URI as the reference URI for the scheme.
     * What this means in practice is that if the resource data contains relative URI references
     * in the same scheme then those URIs will be interpreted relative to this resource's URI.
     * This allows, for example, a configuration to be instantiated from a resource's data, and
     * for that configuration to contain file references relative to the configuration's source
     * file.
     */
    protected URIHandler uriHandler;
    /** The app context. */
    protected Context context;

    /**
     * Create a new resource.
     * @param context       An Android context object (needed for some type conversions).
     * @param data          The resource data.
     * @param uri           The URI used to reference the resource.
     */
    public Resource(Context context, Object data, CompoundURI uri) {
        this.context = context;
        this.data = data;
        this.uri = uri;
    }

    /**
     * Set the resource's URI handler.
     * @param uriHandler    A URI handler with appropriate scheme context URIs.
     */
    protected void setURIHandler(URIHandler uriHandler) {
        this.uriHandler = uriHandler;
    }

    public URIHandler getURIHandler() {
        return uriHandler;
    }

    public CompoundURI getURI() {
        return uri;
    }

    public Context getContext() {
        return context;
    }

    public Object asDefault() {
        return data;
    }

    protected TypeConversions getTypeConversions() {
        return TypeConversions.instanceForContext( context );
    }

    public String asString() {
        return getTypeConversions().asString( data );
    }

    public Number asNumber() {
        return getTypeConversions().asNumber( data );
    }

    public Boolean asBoolean() {
        return getTypeConversions().asBoolean( data );
    }

    public Object asJSONData() {
        return getTypeConversions().asJSONData( data );
    }

    public URI asURL() {
        return getTypeConversions().asURL( data );
    }

    public Drawable asImage() {
        return getTypeConversions().asImage( data );
    }

    public Object asRepresentation(String name) {
        name = name.toLowerCase();
        if( "jsondata".equals( name ) || "json".equals( name ) ) {
            return asJSONData();
        }
        else if( "image".equals( name ) ) {
            return asImage();
        }
        else if( "url".equals( name ) ) {
            return asURL();
        }
        else if( "string".equals( name ) ) {
            return asString();
        }
        else if( "number".equals( name ) ) {
            return asNumber();
        }
        else if( "boolean".equals( name ) ) {
            return asBoolean();
        }
        return getTypeConversions().asRepresentation( data, name );
    }

    /**
     * Refresh the resource by resolving its URI again and returning the result.
     */
    public Resource refresh() {
        return (Resource)uriHandler.dereference( uri );
    }

    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // Two resources are equal if they have the same URI.
        return (obj instanceof Resource) && uri != null && uri.equals( ((Resource)obj).uri );
    }

    @Override
    public String toString() {
        return asString();
    }

}
