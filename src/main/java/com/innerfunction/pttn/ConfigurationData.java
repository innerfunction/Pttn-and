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

/**
 * An interface providing basic access to configuration data.
 * Created by juliangoacher on 29/03/16.
 */
public interface ConfigurationData {

    /**
     * Get a specified representation of a configuration value.
     * @param keyPath           The key path to the required value.
     * @param representation    A representation name. See TypeConversions for standard names.
     * @return The required representation of the value, or null if the value isn't found.
     */
    Object getValue(String keyPath, String representation);

}
