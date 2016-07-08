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
package com.innerfunction.util;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple string template supporting just variable placeholder substitution.
 * Variable placeholders are specified in template strings using curly braces, e.g.
 * kv{variable}kv. Variable values are taken from a key-value encoding compliant data
 * context. Missing values are replaced with an empty string. Placeholders can be
 * escaped by nesting within additional braces, so e.g. kv{{variable}}kv evaluates to
 * the output kv{variable}kv. A % symbol at the start of a placeholder (e.g. kv{%name}kv)
 * indicates that the variable value should be URI escaped.
 */
public class StringTemplate {

    static abstract class Block {
        abstract String eval(Object context, boolean uriEncode);
    }

    /** Regex pattern for matching var references within a template string. */
    static final private Regex Placeholders = new Regex("^([^{]*)([{]+)(%?[-a-zA-Z0-9_$.]+)([}]+)(.*)$");
    /** An array of parsed template blocks. */
    private List<Block> blocks = new ArrayList<>();

    public StringTemplate(String template) {
        while( template.length() > 0 ) {
            String[] matches = Placeholders.matches( template );
            if( matches != null ) {

                String leading   = matches[1];
                String lbraces   = matches[2];
                String reference = matches[3];
                String rbraces   = matches[4];
                String trailing  = matches[5];

                // Append leading text to output.
                this.blocks.add( newTextBlock( leading ) );
                // If just a single opening brace then we have a standard variable placeholder.
                if( lbraces.length() == 1 && rbraces.length() >= 1 ) {
                    boolean uriEncode = false;
                    // A % at the start of the variable reference means that the value result should
                    // be URI encoded.
                    if( reference.charAt( 0 ) == '%' ) {
                        uriEncode = true;
                        reference = reference.substring( 1 );
                    }
                    blocks.add( newRefBlock( reference, uriEncode ) );
                    // Edge case - more trailing braces than leading braces; just append what's left
                    // as a text block.
                    if( rbraces.length() > 1 ) {
                        blocks.add( newTextBlock( rbraces.substring( 1 ) ) );
                    }
                }
                else {
                    // A nested (i.e. escaped) variable placeholder. Strip one each of the opening
                    // and closing braces and append what's left as a plain text block.
                    lbraces = lbraces.substring( 1 );
                    rbraces = rbraces.substring( 1 );
                    String text = lbraces.concat( reference ).concat( rbraces );
                    blocks.add( newTextBlock( text ) );
                }
                template = trailing;
            }
            else {
                int i = template.indexOf('}') + 1;
                if( i > 0 ) {
                    this.blocks.add( this.newTextBlock( template.substring( 0, i ) ) );
                    template = template.substring( i );
                }
                else {
                    this.blocks.add( this.newTextBlock( template ) );
                    break;
                }
            }
        }
    }

    private Block newTextBlock(final String text) {
        return new Block() {
            @Override
            String eval(Object context, boolean uriEncode) {
                return text;
            }
            public String toString() {
                return text;
            }
        };
    }

    private Block newRefBlock(final String ref, final boolean uriEncodeValue) {
        return new Block() {
            @Override
            String eval(Object context, boolean uriEncode) {
                Object value = KeyPath.resolve( ref, context );
                String result = "";
                if( value != null ) {
                    result = value.toString();
                    if( uriEncode || uriEncodeValue ) {
                        result = Uri.encode( result );
                    }
                }
                return result;
            }
            @Override
            public String toString() {
                return "{"+ref+"}";
            }
        };
    }

    public String render(Object context) {
        return render( context, false );
    }

    public String render(Object context, boolean uriEncode) {
        StringBuilder sb = new StringBuilder();
        for( Block b : this.blocks ) {
            sb.append( b.eval( context, uriEncode ));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for( Block b : blocks ) sb.append( b );
        return sb.toString();
    }

    public static StringTemplate templateWithString(String s) {
        return new StringTemplate( s );
    }

    public static String render(String s, Object context) {
        return new StringTemplate( s ).render( context );
    }

    public static String render(String s, Object context, boolean uriEncode) {
        return new StringTemplate( s ).render( context, uriEncode );
    }
}
