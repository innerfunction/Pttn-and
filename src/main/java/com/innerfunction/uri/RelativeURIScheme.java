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

/**
 * An interface implemented by URI scheme handlers which also support relative resource URIs.
 * Attached by juliangoacher on 25/03/16.
 */
public interface RelativeURIScheme extends URIScheme {

    /**
     * Resolve a possibly relative URI against a reference URI.
     * Not all URI schemes support relative URIs, but e.g. file based URIs (@see FileBasedScheme)
     * do allow relative path references in their URIs.
     * Each URI handler maintains a map of reference URIs, keyed by scheme name. When asked to resolve a
     * relative URI, the handler checks for a reference URI in the same scheme, and if one is found then
     * asks the scheme handler to resolve the relative URI against the reference URI.
     * @param uri   The URI to resolve. May be a relative or absolute URI.
     * @param reference The reference URI. Must be an absolute URI.
     */
    public CompoundURI resolveAgainst(CompoundURI uri, CompoundURI reference);

}
