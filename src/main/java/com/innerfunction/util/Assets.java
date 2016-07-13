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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * A class providing simplified access to the Android asset manager.
 */
public class Assets {

    static final String Tag = Assets.class.getSimpleName();

    private AssetManager assetManager;
    private Map<String,Set<String>> assetNamesByPath;

    public Assets(Context context) {
        this.assetManager = context.getAssets();
        this.assetNamesByPath = new HashMap<>();
    }

    /** Open an input stream on a named asset. */
    public InputStream openInputStream(String name) throws IOException {
        return this.assetManager.open( name );
    }

    /** Test whether a name asset exists. */
    public boolean assetExists(String assetName) {
        // Note: following necessary to detect whether the referenced asset exists. This
        // is so as to be consistent in behaviour with the file based URI schemes, which
        // evaluate null if the referenced file doesn't exist.
        String dirPath = Paths.dirname( assetName );
        Set<String> assetPaths = getAssetNamesUnderPath( dirPath );
        return assetPaths.contains( Paths.basename( assetName ) );
    }

    /**
     * Return a set of all asset names under the specified path.
     * If enabled with the cacheAssetNames flag, then lists of names under paths are cached to
     * aid performance.
     * @param path  An assets path.
     * @return A set of asset names under path. All names are relative to path.
     */
    public Set<String> getAssetNamesUnderPath(String path) {
        Set<String> assetNames = null;
        assetNames = this.assetNamesByPath.get( path );
        if( assetNames == null ) {
            try {
                String[] assets = this.assetManager.list( path );
                assetNames = new HashSet<>( Arrays.asList( assets ) );
            }
            catch(IOException e) {
                Log.e(Tag, "Listing assets", e );
                assetNames = new HashSet<>();
            }
            this.assetNamesByPath.put( path, assetNames );
        }
        return assetNames;
    }

}
