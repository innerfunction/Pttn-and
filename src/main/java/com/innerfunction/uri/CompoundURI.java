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

import android.net.Uri;

import com.innerfunction.util.Regex;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing a parsed compound URI string.
 * The URI syntax conforms to the following (partial) BNF:
 *
 *    COMPOUND_URI ::= ( BRACKETED_URI | ALIAS_OR_URI )
 *   BRACKETED_URI ::= '[' ALIAS_OR_URI ']'
 *    ALIAS_OR_URI ::= ( '~' ALIAS | URI )
 *             URI ::= SCHEME ':' NAME? ( '#' FRAGMENT )? PARAMETERS? ( '|' FORMAT )?
 *      PARAMETERS ::= '+' PARAM_NAME PARAM_VALUE PARAMETERS*
 *     PARAM_VALUE ::= ( '=' LITERAL | '@' COMPOUND_URI )
 *           ALIAS ::= '~' NAME ( '|' FORMAT )?
 *          SCHEME ::= (name characters)+
 *            NAME ::= (path characters)+
 *        FRAGMENT ::= (name characters)+
 *         LITERAL ::= (name characters)+
 *          FORMAT ::= (name characters)+
 *
 * Attached by juliangoacher on 25/03/16.
 */
public class CompoundURI {

    /** The URI scheme name. */
    private String scheme;
    /** The name part of the URI. */
    private String name;
    /** The fragment part of the URI. */
    private String fragment;
    /**
     * A map of the URI's parameters keyed by parameter name.
     * Each map entry maps the parameter name to a URI representing the value. (Note that all
     * parameter values can be represented as URIs; literal values are represented using
     * the <i>s:</i> scheme.
     */
    private Map<String,CompoundURI> parameters = new HashMap<>();
    /** The format part of the URI. */
    private String format;
    /**
     * A cached copy of the URIs canonical form. See canonicalForm().
     */
    private String _canonicalForm;

    /** Instantiate a new compound URI from the parsed AST. */
    private CompoundURI(ASTNode ast) throws URISyntaxException {
        initialize( ast );
    }

    /**
     * Instantiate a new compound URI by parsing the provided string.
     * @param input A string containing a compound URI representation.
     * @throws URISyntaxException If the string isn't a valid URI.
     */
    private CompoundURI(String input) throws URISyntaxException {
        ASTNode ast = new ASTNode();
        ast.__input = input;
        if( parseCompoundURI( input, ast ) ) {
            String trailing = ast.__trailing;
            if( trailing != null && trailing.length() > 0 ) {
                int location = input.length() - trailing.length();
                throw new URISyntaxException( input, "Trailing characters after URI", location );
            }
            initialize(ast);
        }
        else if( ast.__error != null ) {
            throw new URISyntaxException( input, ast.__error, ast.__error_location );
        }
        else {
            throw new URISyntaxException( input, "Unable to parse URI", 0 );
        }
    }

    /** Initialize the compound URI instance form the specified AST. */
    private void initialize(ASTNode ast) throws URISyntaxException {
        if( ast.__error != null ) {
            throw new URISyntaxException( ast.__input, ast.__error, ast.__error_location );
        }
        this.scheme = ast.scheme;
        this.name = ast.name;
        this.fragment = ast.fragment;
        this.format = ast.format;
        if( ast.parameters != null ) {
            for( ASTNode param_ast : ast.parameters ) {
                String paramName = param_ast.param_name;
                if( paramName != null ) {
                    CompoundURI paramValue = new CompoundURI( param_ast );
                    parameters.put( paramName, paramValue );
                }
            }
        }
    }

    /**
     * Instantiate a new compound URI as a copy of another URI.
     * Copies all URI parts to the new URI. Makes a copy of the parameter map so that changes
     * to parameters on the original URI don't affect the copy.
     */
    public CompoundURI(CompoundURI uri) {
        this.scheme = uri.scheme;
        this.name = uri.name;
        this.fragment = uri.fragment;
        this.parameters = new HashMap<>( uri.parameters );
        this.format = uri.format;
    }

    /** Instantiate a new compound URI with the specified scheme and name values. */
    public CompoundURI(String scheme, String name) {
        this.scheme = scheme;
        this.name = name;
    }

    /**
     * Instantiate a new compound URI as a copy of another URI, but using a different scheme.
     * @param scheme    The scheme of the new URI.
     * @param uri       The URI to copy.
     */
    public CompoundURI(String scheme, CompoundURI uri) {
        this( uri );
        this.scheme = scheme;
    }

    public String getScheme() {
        return scheme;
    }

    public String getName() {
        return name;
    }

    public String getFragment() {
        return fragment;
    }

    public Map<String,CompoundURI> getParameters() {
        return parameters;
    }

    /**
     * Add a set of parameters to the ones already on this URI.
     * Overwrites any parameters of the same name.
     * @param parameters    A set of additional parameters.
     */
    public void addParameters(Map<String,CompoundURI> parameters) {
        this.parameters.putAll( parameters );
        _canonicalForm = null;
    }

    public String getFormat() { return format; }

    /** Return a copy of the current URI. */
    public CompoundURI copyOf() {
        return new CompoundURI( this );
    }

    /**
     * Return a copy of the current URI but with a modified fragment part.
     * @param fragment  The fragment part for the new URI.
     */
    public CompoundURI copyOfWithFragment(String fragment) {
        CompoundURI copy = new CompoundURI( this );
        copy.fragment = fragment;
        return copy;
    }

    /**
     * Return a copy of the current URI but with a modified name part.
     * @param name  The name part for the new URI.
     */
    public CompoundURI copyOfWithName(String name) {
        CompoundURI copy = new CompoundURI( this );
        copy.name = name;
        return copy;
    }

    /**
     * Generate the canonical representation of the current URI.
     * The canonical form has the following features:
     * <ul>
     *     <li>The scheme, name and fragment parts are URI encoded;</li>
     *     <li>The parameter list is sorted in parameter name order;</li>
     *     <li>All parameter values are represented in their canonical form;</li>
     *     <li>All parameter values are bracketed using square brackets.</li>
     * </ul>
     * Note that two URIs parsed initially from different string representations may share the same
     * canonical form if they are semantically identical.
     * @return A string containing the current URIs canonical form.
     */
    public String canonicalForm() {
        if( _canonicalForm == null ) {
            List<String> paramNames = new ArrayList<>(parameters.keySet());
            Collections.sort(paramNames);
            StringBuilder serializedParams = new StringBuilder();
            for (String paramName : paramNames) {
                CompoundURI paramValue = parameters.get(paramName);
                serializedParams.append('+');
                serializedParams.append(Uri.encode(paramName));
                serializedParams.append("@[");
                serializedParams.append(paramValue.canonicalForm());
                serializedParams.append(']');
            }
            String frag = this.fragment == null ? "" : String.format("#%s", Uri.encode(this.fragment));
            String form = this.format == null ? "" : String.format("|%s", Uri.encode(this.format));
            _canonicalForm = String.format("%s:%s%s%s",
                Uri.encode(scheme), Uri.encode(name), frag, serializedParams, form);
        }
        return _canonicalForm;
    }

    /** Returns the URI's canonical representation. */
    public String toString() {
        return canonicalForm();
    }

    /** Returns the hash code of the URI's canonical representation. */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Compare for equality.
     * A compound URI is equal to another object if that object is also a compound URI, and if their
     * respective canonical forms are identical.
     * @param obj   An object to compare for equality.
     * @return Returns true if <i>obj</i> represents a compound URI which is semantically identical
     * to the current URI.
     */
    public boolean equals(Object obj) {
        return obj instanceof CompoundURI && toString().equals( obj.toString() );
    }

    /**
     * Parse a URI string.
     * @param input A string containing a URI representation.
     * @return The parsed compound URI.
     * @throws URISyntaxException If the input isn't a valid URI.
     */
    public static CompoundURI parse(String input) throws URISyntaxException {
        return new CompoundURI( input );
    }

    /**
     * Try parsing a string that might contain a URI.
     * Swallows the syntax exception if the string isn't a valid URI and returns null instead.
     */
    public static CompoundURI tryParsing(String input) {
        try {
            return parse( input );
        }
        catch(URISyntaxException e) {}
        return null;
    }

    /** A node of the AST produced when parsing a URI. */
    private static class ASTNode {
        String __input;
        String __trailing;
        String __error;
        int __error_location;
        String scheme;
        String name;
        String fragment;
        String param_name;
        List<ASTNode> parameters;
        String format;
        /** Make a child of the current node. */
        ASTNode makeChildNode() {
            ASTNode child = new ASTNode();
            child.__input = this.__input;
            return child;
        }
        /** Record an error message at the specified input. */
        void recordError(String message, String input) {
            this.__error = message;
            this.__error_location = this.__input.length() - input.length();
        }
    }

    // COMPOUND_URI ::= ( BRACKETED_URI | ALIAS_OR_URI )
    private boolean parseCompoundURI(String input, ASTNode ast) {
        return parseBracketedURI( input, ast ) || parseAliasOrURI( input, ast );
    }

    // BRACKETED_URI ::= '[' PLAIN_URI ']'
    private boolean parseBracketedURI(String input, ASTNode ast) {
        if( firstChar( input ) == '[' ) {
            input = input.substring( 1 );
            if( parseURI( input, ast ) ) {
                input = ast.__trailing;
                if( firstChar( input ) == ']') {
                    ast.__trailing = input.substring( 1 );
                    return true;
                }
                else {
                    ast.recordError("Missing closing ]", input );
                }
            }
        }
        return false;
    }

    // ALIAS_OR_URI ::= ( '~' ALIAS | URI )
    private boolean parseAliasOrURI(String input, ASTNode ast) {
        return parseAlias( input, ast ) || parseURI( input, ast );
    }

    // ALIAS ::= '~' NAME ( '|' FORMAT )?
    private boolean parseAlias(String input, ASTNode ast) {
        if( firstChar( input ) == '~' ) {
            input = input.substring( 1 );
            if( parseName( input, ast ) ) {
                input = ast.__trailing;
                // e.g. convert ~name => a:name
                ast.scheme = "a";
                if( parseFormat( input, ast ) ) {
                    input = ast.__trailing;
                }
                return true;
            }
        }
        return false;
    }

    // URI ::= SCHEME ':' NAME? ( '#' FRAGMENT )? PARAMETERS? ( '|' FORMAT )?
    private boolean parseURI(String input, ASTNode ast) {
        if( parseScheme( input, ast ) ) {
            input = ast.__trailing;
            if( firstChar( input ) == ':' ) {
                input = input.substring( 1 );
                if( parseName( input, ast ) ) {
                    input = ast.__trailing;
                }
                if( firstChar( input ) == '#' ) {
                    input = input.substring( 1 );
                    if( parseFragment( input, ast ) ) {
                        input = ast.__trailing;
                    }
                }
                ast.parameters = new ArrayList<>();
                ASTNode param_ast = new ASTNode();
                while( parseParameters( input, param_ast ) ) {
                    ast.parameters.add( param_ast );
                    input = param_ast.__trailing;
                    param_ast = new ASTNode();
                }
                if( parseFormat( input, ast ) ) {
                    input = ast.__trailing;
                }
                ast.__trailing = input;
                return true;
            }
        }
        return false;
    }

    // Match any word characters
    private boolean parseScheme(String input, ASTNode ast) {
        String[] groups = Regex.matches("^(\\w+)(.*)$", input );
        if( groups != null && groups.length > 2 ) {
            ast.scheme = groups[1];
            ast.__trailing = groups[2];
            return true;
        }
        return false;
    }

    // Match any word characters or . , / % kv ~ { } -
    private boolean parseName(String input, ASTNode ast) {
        String[] groups = Regex.matches("^([\\w.,/%kv~{}-]*)(.*)$", input );
        if( groups != null && groups.length > 2 ) {
            ast.name = groups[1];
            ast.__trailing = groups[2];
            return true;
        }
        return false;
    }

    // Match any word characters or . / % kv ~ -
    private boolean parseFragment(String input, ASTNode ast) {
        String[] groups = Regex.matches("^([\\w./%kv~-]*)(.*)$", input );
        if( groups != null && groups.length > 2 ) {
            ast.fragment = groups[1];
            ast.__trailing = groups[2];
            return true;
        }
        return false;
    }

    // PARAMETERS ::= '+' PARAM_NAME ( '@' URI | '=' LITERAL ) PARAMETERS*
    private boolean parseParameters(String input, ASTNode ast) {
        if( firstChar( input ) == '+' ) {
            input = input.substring( 1 );
            if( parseParamName( input, ast ) ) {
                input = ast.__trailing;
                char prefix = firstChar( input );
                if( prefix == '@' ) {
                    input = input.substring( 1 );
                    return parseCompoundURI( input, ast );
                }
                else if( prefix == '=' ) {
                    input = input.substring( 1 );
                    if( parseParamLiteral( input, ast ) ) {
                        // Convert the literal value to the AST for a string scheme URI.
                        // Note that the param literal value is placed into the 'name' property
                        // of the AST node.
                        ast.scheme = "s";
                        return true;
                    }
                }
                else {
                    ast.recordError("Expected @ or =", input );
                }
            }
        }
        return false;
    }

    // // Match | followed by any format characters or . _ ~ -
    private boolean parseFormat(String input, ASTNode ast) {
        if( firstChar( input ) == '|' ) {
            input = input.substring( 1 );
            String[] groups = Regex.matches("^([\\w\\._~-]*)(.*)$", input );
            if( groups != null && groups.length > 2 ) {
                ast.format = groups[1];
                ast.__trailing = groups[2];
                return true;
            }
        }
        return false;
    }

    // Match an optional * prefix followed by any word characters or -
    private boolean parseParamName(String input, ASTNode ast) {
        String[] groups = Regex.matches("^(\\*?[\\w-]+)(.*)$", input );
        if( groups != null && groups.length > 2 ) {
            ast.param_name = groups[1];
            ast.__trailing = groups[2];
            return true;
        }
        return false;
    }

    // Match any characters which aren't + | or ]
    private boolean parseParamLiteral(String input, ASTNode ast) {
        String[] groups = Regex.matches("^([^+|\\]]*)(.*)$", input );
        if( groups != null && groups.length > 2 ) {
            ast.name = groups[1];
            ast.__trailing = groups[2];
            return true;
        }
        return false;
    }

    /**
     * Return the first character in a string.
     * Handles null and empty strings, returning 0x00 instead.
     */
    static final char firstChar(final String s) {
        return s != null && s.length() > 0 ? s.charAt( 0 ) : 0x00;
    }

}
