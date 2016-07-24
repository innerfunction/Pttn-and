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

import com.innerfunction.util.TypeConversions;

import java.util.Map;

/**
 * An internal URI handler for the repr: scheme.
 * This scheme allows URI resources to be coerced to a specific representation within a URI.
 * Scheme URIs are in the form:
 * <pre>
 *     {scheme name}:{representation name}+value@{uri}
 * </pre>
 * That is, each URI should have a value parameter specifying the value to be coerced, and a URI
 * name part that identifies the name of the required representation.
 * This scheme is useful only in very particular cases, e.g. where the default resolved representation
 * isn't what is actually needed.
 *
 * Attached by juliangoacher on 26/03/16.
 */
public class ReprScheme implements URIScheme {

    private TypeConversions typeConversions;

    public ReprScheme(Context context) {
        typeConversions = TypeConversions.instanceForContext( context );
    }

    @Override
    public Object dereference(CompoundURI uri, Map<String, Object> params) {
        // The URI specifies the name of the require representation.
        String repr = uri.getName();
        // The value who's representation we want.
        Object value = params.get("value");
        if( value instanceof Resource ) {
            // If the value is a resource then use the Resource API to do the conversion.
            value = ((Resource)value).asRepresentation( repr );
        }
        else {
            // Else use standard type conversions.
            value = typeConversions.asRepresentation( value, repr );
        }
        return value;
    }
}
