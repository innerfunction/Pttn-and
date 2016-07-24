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

import java.util.Map;

/**
 * An interface implemented by classes which handle URIs in a particular URI scheme.
 * Attached by juliangoacher on 25/03/16.
 */
public interface URIScheme {

    /**
     * Dereference a URI.
     * @param uri The parsed URI to be dereferenced.
     * @param params A map of the URI's parameter name and values. All parameters have their
     * URI values dereferenced to their actual values.
     * @return The value referenced by the URI.
     */
    Object dereference(CompoundURI uri, Map<String,Object> params);

}
