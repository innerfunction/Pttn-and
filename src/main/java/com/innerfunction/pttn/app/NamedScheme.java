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
package com.innerfunction.pttn.app;

import com.innerfunction.pttn.Container;
import com.innerfunction.pttn.PendingNamed;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.URIScheme;
import com.innerfunction.util.KeyPath;

import java.util.Map;

/**
 * An internal URI handler for the named: scheme.
 * The named: scheme allows named components of the app container to be accessed via URI.
 * The scheme handler forwards requests to the getNamed() method of the app container.
 *
 * Attached by juliangoacher on 30/03/16.
 */
public class NamedScheme implements URIScheme {

    private Container container;

    public NamedScheme(Container container) {
        this.container = container;
    }

    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        // Break the named reference into the initial name and a trailing path.
        // e.g. 'object.sub.property' -> name = 'object' path = 'sub.property'
        String name = uri.getName(), path = null;
        int idx = name.indexOf('.');
        if( idx > -1 ) {
            path = name.substring( idx + 1 );
            name = name.substring( 0, idx );
        }
        // Get the named object.
        Object result = container.getNamed( name );
        // If a path is specified then evaluate that on the named object.
        if( result != null && path != null ) {
            // Check for pending names. These are only returned during the container's
            // configuration cycle, and are used to resolve circular dependencies. When
            // these are returned then just the path needs to be recorded.
            if( result instanceof PendingNamed ) {
                ((PendingNamed)result).setReferencePath( path );
            }
            else {
                result = KeyPath.resolve( path, result );
            }
        }
        return result;
    }

}
