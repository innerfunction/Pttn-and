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

import android.content.Context;
import android.util.Log;

import com.innerfunction.util.Paths;

import java.io.File;
import java.util.Map;

public class FileBasedScheme implements RelativeURIScheme {

    private static final String LogTag = FileBasedScheme.class.getSimpleName();
    /** The app context. */
    protected Context context;
    /**
     * The scheme's root directory.
     * All file's referenced using the scheme handled by this class will be located under this root.
     */
    protected File rootDir;

    /** Create a scheme handler for the file system root. */
    protected FileBasedScheme(Context context) {
        this.context = context;
        this.rootDir = new File("/");
    }

    /**
     * Create a new scheme handler for the specified root path.
     * @param context   The app context.
     * @param rootPath  The root path.
     */
    public FileBasedScheme(Context context, String rootPath) {
        this.context = context;
        this.rootDir = new File( rootPath );
    }

    /**
     * Create a new scheme handler for the specified root directory.
     * @param context   The app context.
     * @param rootDir   The root directory.
     */
    public FileBasedScheme(Context context, File rootDir) {
        this.context = context;
        this.rootDir = rootDir;
    }

    /**
     * Return the root path for this scheme.
     * Returns null if no root path is specified.
     */
    public String getRootPath() {
        return rootDir != null ? rootDir.getAbsolutePath() : null;
    }

    /**
     * Test if a URI is a relative URI, and if so then resolve to an absolute URI.
     * Both URI arguments should belong to the same URI scheme, which should be the scheme
     * handled by this handler. The reference URI must be an absolute URI.
     * @param uri           The URI to resolve.
     * @param reference     An absolute URI to use as the reference.
     * @return An absolute URI referencing the same resource as the uri argument.
     */
    @Override
    public CompoundURI resolveAgainst(CompoundURI uri, CompoundURI reference) {
        // If URI name doesn't begin with / then it is a relative URI.
        String name = uri.getName();
        if( name.length() > 0 && name.charAt( 0 ) != '/' ) {
            String contextDir = Paths.dirname( reference.getName() );
            String absPath = Paths.join( contextDir, name );
            uri = uri.copyOfWithName( absPath );
        }
        return uri;
    }

    /**
     * Dereference a URI to a file resource.
     * Attempts to map the URI name to a file path relative to the URI handler's root path. If a
     * file exists at that file path then returns a file resource representing that file; else
     * returns null.
     * @param uri The parsed URI to be dereferenced.
     * @param params A map of the URI's parameter name and values. All parameters have their
     * URI values dereferenced to their actual values.
     * @return A file resource, or null if the file doesn't exist.
     */
    @Override
    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        Resource result = null;
        String name = uri.getName();
        if( name.length() > 0 && name.charAt( 0 ) == '/' ) {
            name = name.substring( 1 );
        }
        File file = new File( this.rootDir, name );
        Log.d(LogTag, String.format("%s -> %s", uri, file.getAbsolutePath() ) );
        if( file.exists() ) {
            result = new FileResource( this.context, file, uri );
        }
        else{
            Log.d( LogTag, "File not found: " + uri );
        }
        return result;
    }

}

