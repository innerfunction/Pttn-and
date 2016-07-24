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
package com.innerfunction.http;

import com.innerfunction.q.Q;

/**
 * An interface to be implemented by classes providing HTTP authentication related functionality.
 *
 * Attached by juliangoacher on 09/07/16.
 */
public interface AuthenticationDelegate {

    /** Test whether a response represents an authentication error. */
    boolean isAuthenticationErrorResponse(Client client, Response response);

    /**
     * Perform an authentication.
     * TODO: A more complete API design would provide info on the domain/realm that authentication is required for.
     */
    Q.Promise<Response> authenticateUsingHTTPClient(Client client);

}
