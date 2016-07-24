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

import java.util.List;

/**
 * An interface for de-referencing compound URIs to their referenced resources or values.
 * Attached by juliangoacher on 25/03/16.
 */
public interface URIHandler {

    /**
     * Dereference a URI to a resource.
     * @param uri A parsed compound URI. @see CompoundURI.
     * @return The deferenced value. Can be null.
     */
    Object dereference(CompoundURI uri);

    /**
     * Dereference a URI to a resource.
     * @param uri An unparsed compound URI string. @see CompoundURI.
     * @return The deferenced value. Can be null.
     */
    Object dereference(String uri);

    /**
     * Return a new URI handler with a modified scheme context (used to dereference relative URIs).
     */
    URIHandler modifySchemeContext(CompoundURI uri);

    /**
     * Return a copy of this URI handler with a replacement scheme handler.
     */
    URIHandler replaceURIScheme(String schemeName, URIScheme scheme);

    /**
     * Test if the uriHandler has a registered handler for the named scheme.
     * @param scheme A scheme name.
     * @return Returns true if the scheme name is recognized.
     */
    boolean hasHandlerForURIScheme(String scheme);

    /**
     * Add a scheme handler.
     * @param scheme The name the scheme handler will be bound to.
     * @param handler The new scheme handler.
     */
    void addHandlerForScheme(String scheme, URIScheme handler);

    /**
     * Return a list of all registered scheme names.
     */
    List<String> getURISchemeNames();

    /**
     * Get the handler for a named scheme.
     */
    URIScheme getHandlerForURIScheme(String scheme);

}
