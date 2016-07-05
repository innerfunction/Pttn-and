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
 * An interface implemented by components which represent services.
 * A service is something which needs clear start/stop semantics in order to work properly.
 * A service is started as soon as the container it belongs to is started. If a service is instantiated
 * in an already running container then it is started immediately. Services are stopped when the
 * container is stopped.
 */
public interface Service {

    /** Signal that the service should be started. */
    void startService();

    /** Signal that the service should be stopped. */
    void stopService();

}
