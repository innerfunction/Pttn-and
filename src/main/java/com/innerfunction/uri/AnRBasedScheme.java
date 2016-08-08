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

import com.innerfunction.util.Assets;
import com.innerfunction.util.Paths;

import java.util.Map;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

/**
 * URI scheme handler for resources loaded from the app's assets or res directories.
 * @author juliangoacher
 */
public class AnRBasedScheme extends FileBasedScheme {

    static final String LogTag = AnRBasedScheme.class.getSimpleName();
    /** The app's resources. */
    private Resources r;
    /** The app's package name. */
    private String packageName;
    /** The app's assets. */
    private Assets assets;

    public AnRBasedScheme(Context context, Assets assets) {
        this( context, "", assets );
    }

    public AnRBasedScheme(Context context, String rootPath, Assets assets) {
        super( context, rootPath );
        this.r = context.getResources();
        this.packageName = context.getPackageName();
        this.assets = assets;
    }

    @Override
    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        Resource result = null;
        // Leading slashes on an asset name will cause problems when resolving the asset file, so
        // strip them from the name.
        String name = uri.getName();
        if( name.length() > 0 && name.charAt( 0 ) == '/' ) {
            name = name.substring( 1 );
        }
        // The URI fragment can be used to specify one of the standard Android resource types.
        // This should not normally be necessary (for example, the code can recognize drawable
        // resource types from the file extension on the resource name) but sometimes is, e.g.
        // when referencing a shape drawable in an XML file.
        String resourceType = uri.getFragment();
        int resourceID = getResourceIDForName( name, resourceType );
        if( resourceID != 0 ) {
            Log.d(LogTag,String.format("Accessing %s from resources using ID %d", name, resourceID ));
            result = new AnRResource.Res( context, resourceID, uri );
        }
        else {
            String assetName = Paths.join( getRootPath(), name );
            if( assetName.charAt( 0 ) == '/' ) {
                assetName = assetName.substring( 1 );
            }
            // TODO This needs to be reviewed - currently it is a hack to allow the content
            // TODO container to be configured with a base content path which is an app: URI.
            if( "url".equals( resourceType ) ) {
                return "file:///android_asset/"+assetName;
            }
            if( assets.assetExists( assetName ) ) {
                // Asset name found at specified location, so return an asset resource.
                result = new AnRResource.Asset( context, assetName, assets, uri );
            }
        }
        return result;
    }

    /** Get an Android resource ID for a URI name. */
    public int getResourceIDForName(String name, String resourceType) {
        if( resourceType == null ) {
            // No resource type specified, try deriving from the asset name file extension.
            String ext = Paths.extname( name );
            if( ".png".equals( ext ) || ".jpg".equals( ext ) || ".jpeg".equals( ext ) ) {
                resourceType = "drawable";
            }
            else {
                resourceType = "string";
            }
        }
        // Build a resource ID by -
        // * stripping any file extension from the asset name;
        // * converting / to __
        // * converting - to _
        // This will convert a name like ep/icons/icon-schedule.png to ep__icons__icon_schedule
        String resourceID = Paths.stripext( name ).replace("/", "__").replace("-", "_");
        return this.r.getIdentifier( resourceID, resourceType, packageName );
    }
}
