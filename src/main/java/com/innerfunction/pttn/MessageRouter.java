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
 * An interface for routing messages to named targets.
 * Components which contain addressable message targets should implement this interface.
 * Attached by juliangoacher on 29/03/16.
 */
public interface MessageRouter {

    /**
     * Try routing a message to a message target within the current component.
     * @param message   The message to be dispatched.
     * @param sender    The component sending the message.
     * @return Returns true if the message was routed to a message receiver which accepted and
     * processed the message.
     */
    boolean routeMessage(Message message, Object sender);

}
