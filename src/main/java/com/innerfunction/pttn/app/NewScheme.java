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

import com.innerfunction.pttn.Configuration;
import com.innerfunction.pttn.Container;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.URIScheme;

import java.util.Map;

/**
 * An internal URI handler for the _new:kv scheme.
 * The new: scheme allows new components to be instantiated using a URI. The URI's 'name'
 * part specified the type or class name of the object to be instantiated. Dependency injection
 * is then performed using the URI's parameters as configuration values.
 *
 * Attached by juliangoacher on 30/03/16.
 */
public class NewScheme implements URIScheme {

    private Container container;

    public NewScheme(Container container) {
        this.container = container;
    }

    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        String typeName = uri.getName();
        Configuration config = container.makeConfiguration( params ).normalize();
        Object result = container.newInstanceForTypeNameAndConfiguration( typeName, config, false );
        if( result == null ) {
            // If instantiation fails (i.e. because the type name isn't recognized) then try
            // instantiating from class name.
            result = container.newInstanceForClassNameAndConfiguration( typeName, config );
        }
        if( result != null ) {
            container.configureObject( result, config, uri.toString() );
        }
        return result;
    }

}
