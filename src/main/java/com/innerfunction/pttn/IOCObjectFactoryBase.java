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

import java.util.Map;

/**
 * Utility base class for IOC object factory instances.
 * Supports a factory class style which uses a parameterized, partial base configuration which
 * is extended with a configuration provided by the container to yield the full object configuration.
 *
 * Created by juliangoacher on 29/03/16.
 */
public class IOCObjectFactoryBase<T> implements IOCObjectFactory {

    /**
     * A default base configuration for instances produced by this class.
     * Typically minimal implementation should contain a *type or *ios-class property.
     */
    private Configuration baseConfiguration;

    /**
     * Initialize the factory with a base configuration.
     * The base configuration is a parameterized partial-configuration which will be resolved with values
     * from the container.
     */
    public IOCObjectFactoryBase(Configuration baseConfiguration) {
        this.baseConfiguration = baseConfiguration;
    }

    public T buildObject(Configuration configuration, Container container, Map<String,Object> parameters, String identifier) {
        // Flatten the object configuration.
        configuration = configuration.flatten();
        // Extend the object configuration from the base configuration.
        configuration = configuration.mixoverConfiguration( baseConfiguration );
        // If any parameters then extend the configuration using them.
        if( parameters != null ) {
            configuration = configuration.extendWithParameters( parameters );
        }
        // Ask the container to build the object, then return the result.
        return (T)container.buildObject( configuration, identifier, false );
    }

    public T buildObject(Configuration configuration, Container container, String identifier) {
        return buildObject( configuration, container, null, identifier );
    }

}
