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
 *  * A message sent between two components within a container.
 * Attached by juliangoacher on 29/03/16.
 */
public class Message {

    /**
     * The message name.
     */
    private String name;
    /**
     * The message parameters.
     */
    private Map<String,Object> parameters;
    /**
     * The message target.
     */
    private String target;
    /**
     * The message's target path split into path components.
     */
    private String[] targetPath;

    public Message(String name, Map<String,Object> parameters) {
        this( new String[0], name, parameters );
    }

    public Message(String target, String name, Map<String,Object> parameters) {
        if( target == null ) {
            this.target = "";
            this.targetPath = new String[0];
        }
        else {
            this.target = target;
            this.targetPath = target.split( "\\." );
        }
        this.name = name;
        this.parameters = parameters;
    }

    public Message(String[] targetPath, String name, Map<String,Object> parameters) {
        if( targetPath == null ) {
            targetPath = new String[0];
        }
        StringBuilder sb = new StringBuilder();
        for( String target : targetPath ) {
            if( sb.length() > 0 ) {
                sb.append('.');
            }
            sb.append( target );
        }
        this.target = sb.toString();
        this.targetPath = targetPath;
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    /** Get a named action parameter value. */
    public Object getParameter(String name) {
        return parameters.get( name );
    }

    /** Test if the message has the specified name. */
    public boolean hasName(String name) {
        return this.name.equals( name );
    }

    /** Test whether the message has an empty target. */
    public boolean hasEmptyTarget() {
        return targetPath.length == 0;
    }

    /** Test if the (entire) target matches the specified string. */
    public boolean hasTarget(String target) {
        return this.target.equals( target );
    }

    /** Get the target name at the head of the target path. */
    public String targetHead() {
        return targetPath.length > 0 ? targetPath[0] : null;
    }

    /**
     * Pop the head name from the target path and return a new message whose target path is the remainder.
     * Return null if there is no trailing target path.
     */
    public Message popTargetHead() {
        if( targetPath.length > 0 ) {
            String[] newTargetPath = new String[targetPath.length - 1];
            System.arraycopy( targetPath, 1, newTargetPath, 0, newTargetPath.length );
            return new Message( newTargetPath, name, parameters );
        }
        return null;
    }

}
