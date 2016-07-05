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
 * An interface to be implemented by components capable of receiving messages from other components.
 * Created by juliangoacher on 29/03/16.
 */
public interface MessageReceiver {

    /**
     * Ask a target to receive a message.
     * @param message   The message to receive.
     * @param sender    The component which sent the message.
     * @return Returns true if the message was processed by the target, false otherwise.
     * A true result will stop further processing of the message. A false result may mean
     * that the message continues to be dispatched to other components (i.e. until some component
     * handles it).
     */
    boolean receiveMessage(Message message, Object sender);

}
