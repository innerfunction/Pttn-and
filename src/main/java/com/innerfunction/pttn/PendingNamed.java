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

import com.innerfunction.util.KeyPath;
import static com.innerfunction.pttn.ObjectConfigurer.Properties;

/**
 * A placeholder value used to represent a deferred named value.
 * Deferred names happen when circular dependencies are detected. In such cases, the named
 * value can't be resolved till after its configuration is complete. This placeholder allows
 * the details of the named dependency to be recorded so that it can be resolved after the
 * configuration cycle has completed.
 */
public class PendingNamed {

    /** A key value to use when tracking this pending in different container dictionaries. */
    private ObjectKey objectKey;
    /** The property key, e.g. property name; or array index or dictionary key. */
    private String key;
    /** The key path of the property value on the named object. */
    private String referencePath;
    /**
     * The properties of named object.
     * Used to identify the object and to set its named properties.
     */
    private Properties properties;
    /** The object configurer waiting for the pending value. */
    private ObjectConfigurer configurer;

    /**
     * Provide information to the pending named needed to complete the named's configuration.
     * @param properties    Properties of the
     * @param configurer
     */
    public void setConfigurationContext(Properties properties, ObjectConfigurer configurer) {
        this.properties = properties;
        this.configurer = configurer;
        this.objectKey = new ObjectKey( properties.getPropertyOwner() );
    }

    /**
     * Test whether the pending has a configurer waiting for the result.
     * Not all pendings are used - some are discarded (e.g. when attempting to resolve a
     * configuration) and so don't need to be completed when resolved.
     */
    public boolean hasWaitingConfigurer() {
        return configurer != null;
    }

    /** Complete the pending by notifying the waiting configurer of the value result. */
    public Object completeWithValue(Object value) {
        // If a reference path is set then use it to fully resolve the pending value on the named
        // object.
        if( referencePath != null ) {
            value = KeyPath.resolve( referencePath, value );
        }
        return configurer.injectPropertyValue( key, properties, value );
    }

    public Object getObjectKey() {
        return objectKey;
    }

    public Object getObject() {
        return properties.getPropertyOwner();
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setReferencePath(String referencePath) {
        this.referencePath = referencePath;
    }

}
