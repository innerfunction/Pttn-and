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
package com.innerfunction.pttn.ui.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;

import com.innerfunction.uri.Resource;
import com.innerfunction.uri.URIHandler;
import com.innerfunction.util.Images;
import com.innerfunction.util.TypeConversions;
import com.innerfunction.util.ValueMap;

import com.nakardo.atableview.foundation.NSIndexPath;

/**
 * Data source for table views.
 * Attached by juliangoacher on 05/05/16.
 */
@SuppressLint("DefaultLocale")
@SuppressWarnings("rawtypes")
public class TableData {

    static final String Tag = TableData.class.getSimpleName();

    /** The image cache size, in MB. */
    static final int CacheSize = 2 * 1024 * 1024;
    /**
     * A shared cache of table row images.
     * Note on thread safety: All image operations which access the cache will be performed from the
     * UI thread (i.e. as a table row is being created); this is effectively single-threaded
     * operation, so no synchronization is necessary before calling cache methods.
     */
    static final LruCache<String,Drawable> ImageCache = new LruCache<String,Drawable>( CacheSize ) {
        @Override
        protected int sizeOf(String key, Drawable image) {
            // NOTE Drawable doesn't provide any method for getting the image size, so instead
            // estimate the size assuming a 32 bit (4 byte) pixel depth.
            return image.getIntrinsicWidth() * image.getIntrinsicHeight() * 4;
        }
    };

    /** The table data. May represent either grouped or non-grouped data. */
    private List data;
    /** The visible table data, i.e. after a filter has been applied. */
    private List visibleData;
    /** A list of the section tables, for grouped data. */
    private List<String> sectionTitles;
    /** A flag indicating whether the data is grouped or not. */
    private boolean grouped;
    /** A list of data fields to check when filtering by a search term. */
    private List<String> searchFieldNames;
    /** A delegate object for modifying resolved data values. */
    private TableDataDelegate delegate;
    /** An object for handling type conversions. */
    private TypeConversions typeConversions;
    /** A handler for resolving internal URIs. */
    private URIHandler uriHandler;

    public TableData(Context context) {
        data = new ArrayList();
        visibleData = data;
        sectionTitles = new ArrayList<>();
        grouped = false;
        searchFieldNames = Arrays.asList("title", "description");
        typeConversions = TypeConversions.instanceForContext( context );
    }

    public void reset() {
        data = Collections.emptyList();
        clearFilter();
    }

    public void setTableDataDelegate(TableDataDelegate tableDataDelegate) {
        delegate = tableDataDelegate;
    }

    public void setURIHandler(URIHandler uriHandler) {
        this.uriHandler = uriHandler;
    }

    /**
     * Set the table data.
     *
     * @param listData
     */
    @SuppressWarnings("unchecked")
    public void setData(List listData) {
        // Test whether the data is grouped or non-grouped. If grouped, then extract section header
        // titles from the data.
        // This method allows grouped data to be presented in one of two ways, and assumes that the
        // data is grouped
        // consistently throughout.
        // * The first grouping format is as an array of arrays. The section header title is
        // extracted as the first character
        // of the title of the first item in each group.
        // * The second grouping format is as an array of dictionarys. Each dictionary represents a
        // section object with
        // 'sectionTitle' and 'sectionData' properties.
        // Data can also be presented as an array of strings, in which case each string is used as a
        // row title.
        int dataSize = listData.size();
        Object firstItem = dataSize > 0 ? listData.get(0) : null;

        // Allow a table section to be represented using a List or String array.
        if( firstItem instanceof List || firstItem instanceof String[] ) {
            grouped = true;
            List<String> titles = new ArrayList<>( dataSize );
            for( Object section : listData ) {
                Map row = (Map)((List)section).get( 0 );
                if (row != null) {
                    titles.add( (String)row.get("title") );
                }
                else {
                    titles.add("");
                }
            }
            data = listData;
            sectionTitles = titles;
        }
        else if( firstItem instanceof Map ) {
            Map<String,Object> sectionDef = (Map<String,Object>)firstItem;
            if( sectionDef.containsKey("sectionTitle") || sectionDef.containsKey("sectionData") ) {
                grouped = true;
                List<String> titles = new ArrayList<>( dataSize );
                List sections = new ArrayList( dataSize );
                for( Object section : listData ) {
                    Map sectionMap = (Map)section;
                    String sectionTitle = null;
                    if( delegate != null ) {
                        sectionTitle = delegate.resolveTableDateSectionTitle( sectionMap, this );
                    }
                    if( sectionTitle == null ) {
                        sectionTitle = (String)sectionMap.get("sectionTitle");
                    }
                    titles.add( sectionTitle != null ? sectionTitle : "" );
                    List sectionData = (List)sectionMap.get("sectionData");
                    sections.add( sectionData != null ? sectionData : new ArrayList() );
                }
                data = sections;
                sectionTitles = titles;
            }
            else {
                grouped = false;
                data = listData;
            }

        }
        else if( firstItem instanceof String ) {
            grouped = false;
            List<Map<String,String>> rows = new ArrayList<>( dataSize );
            for( Object title : listData ) {
                Map<String,String> row = new HashMap<>();
                row.put("title", (String)title );
                rows.add( row );
            }
            data = rows;
        }
        else {
            grouped = false;
            data = new ArrayList();
        }
        visibleData = data;
    }

    /**
     * Get row data for an index path.
     */
    public ValueMap getRowDataForIndexPath(NSIndexPath indexPath) {
        Map rowData = null;
        int section = indexPath.getSection();
        int row = indexPath.getRow();
        if( visibleData.size() > 0 ) {
            if( grouped ) {
                if( visibleData.size() > section ) {
                    List sectionData = (List)visibleData.get( section );
                    if( sectionData.size() > row ) {
                        rowData = (Map)sectionData.get( row );
                    }
                }
            }
            else if( visibleData.size() > row ) {
                rowData = (Map)visibleData.get( row );
            }
        }
        if( rowData == null ) {
            rowData = new HashMap();
        }
        return new ValueMap( rowData, typeConversions );
    }

    /**
     * Find the index path of the first row with the specified value in the specified field.
     * @param value     The value being looked for.
     * @param fieldName The name of the field containing the required value.
     * @return The index path of the first matching row, or null if no row is found.
     */
    public NSIndexPath getIndexPathForFirstRowWithFieldValue(Object value, String fieldName) {
        if( grouped ) {
            for( int s = 0; s < visibleData.size(); s++ ) {
                List sectionData = (List)visibleData.get( s );
                for( int r = 0; r < sectionData.size(); r++ ) {
                    Map rowData = (Map)sectionData.get( r );
                    Object fieldValue = rowData.get( fieldName );
                    if( fieldValue != null && fieldValue.equals( value ) ) {
                        return NSIndexPath.indexPathForRowInSection( r, s );
                    }
                }
            }
        }
        else for( int r = 0; r < visibleData.size(); r++ ) {
            Map rowData = (Map)visibleData.get( r );
            Object fieldValue = rowData.get( fieldName );
            if( fieldValue != null && fieldValue.equals( value ) ) {
                return NSIndexPath.indexPathForRowInSection( r, 1 );
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return data.size() == 0;
    }

    /**
     * Return the number of sections in the table data.
     *
     * @return
     */
    public int getSectionCount() {
        if( visibleData.size() > 0 ) {
            return grouped ? visibleData.size() : 1;
        }
        return 0;
    }

    /**
     * Return the number of rows in the specified section.
     *
     * @param section
     * @return
     */
    public int getSectionSize(int section) {
        int size = 0;
        if( visibleData.size() > 0 ) {
            if( grouped ) {
                if( visibleData.size() > section ) {
                    List sectionData = (List)visibleData.get( section );
                    size = sectionData.size();
                }
                else {
                    size = 0;
                }
            }
            else if( section == 0 ) {
                size = visibleData.size();
            }
        }
        return size;
    }

    /** Interface for wrapping table data filter predicates. */
    public interface FilterPredicate {
        boolean testRow(Map row);
    }

    @SuppressWarnings("unchecked")
    public void filterBy(FilterPredicate predicate) {
        List result = new ArrayList();
        if( grouped ) {
            for( Object section : data ) {
                List<Map> filteredSection = new ArrayList<>();
                for( Map row : (List<Map>)section ) {
                    if( predicate.testRow( row ) ) {
                        filteredSection.add( row );
                    }
                }
                result.add( filteredSection );
            }
        }
        else for( Object rowObj : data ) {
            Map row = (Map)rowObj;
            if( predicate.testRow( row ) ) {
                result.add( row );
            }
        }
        this.visibleData = result;
    }

    public void filterBy(String searchTerm) {
        filterBy( searchTerm, null );
    }

    public void filterBy(String searchTerm, String scope) {
        // Convert search term to lower case (for a case insensitive match).
        final String lcSearchTerm = searchTerm.toLowerCase();
        // The list of data fields to include in the search.
        final List<String> searchNames = (scope != null) ? Arrays.asList( scope ) : searchFieldNames;
        // The filter predicate.
        FilterPredicate predicate = new FilterPredicate() {
            @Override
            public boolean testRow(Map row) {
                for( String name : searchNames ) {
                    Object value = row.get( name );
                    if( value instanceof String && ((String)value).toLowerCase().contains( lcSearchTerm ) ) {
                        return true;
                    }
                }
                return false;
            }
        };
        // Perform the search.
        filterBy( predicate );
    }

    public void clearFilter() {
        visibleData = data;
    }

    // ==== Setters and Getters
    public void setSearchFieldNames(List<String> searchFieldNames) {
        this.searchFieldNames = searchFieldNames;
    }

    public void setSearchFieldNames(String... searchFieldNames) {
        this.searchFieldNames = Arrays.asList( searchFieldNames );
    }

    public String getSectionTitle(int section) {
        if( section < sectionTitles.size() ) {
            return sectionTitles.get( section );
        }
        return null;
    }

    // Image handlers.

    public Drawable loadImage(String imageName) {
        return loadImage( imageName, null );
    }

    public Drawable loadImage(String imageName, Drawable defaultImage) {
        if( imageName == null ) {
            return null;
        }
        Drawable result = ImageCache.get( imageName );
        if( result == null ) {
            // Image names beginning with '@' are URI references to the image.
            if( imageName.startsWith("@") && uriHandler != null ) {
                // TODO Note that currently the URI handler is set by the table view *only* when
                // TODO the table data is loaded from a URI which dereferences to a resource;
                // TODO To fix, the uriHandler should default to the global handler initially.
                String imageURI = imageName.substring( 1 );
                Object imageRsc = uriHandler.dereference( imageURI );
                if( imageRsc instanceof Resource ) {
                    result = ((Resource)imageRsc).asImage();
                }
                else if( imageRsc instanceof Drawable ) {
                    result = (Drawable)imageRsc;
                }
                else {
                    Log.w( Tag, String.format("Image resource not found: %s", imageURI ) );
                }
            }
            else {
                result = typeConversions.asImage( imageName );
            }
        }
        if( result != null ) {
            ImageCache.put( imageName, result );
        }
        if( result == null ) {
            result = defaultImage;
        }
        return result;
    }

    public Drawable loadRoundedImage(String imageName, float radius) {
        String cacheKey = String.format("rounded.%s.radius.%f", imageName, radius );
        Drawable rounded = ImageCache.get( cacheKey );
        Log.d(Tag, String.format("%s %s", cacheKey, rounded == null ? "miss" : "hit"));
        if( rounded == null ) {
            // Add rounded corners to image
            // NOTE: This code currently displays the image as a complete circle.
            BitmapDrawable image = (BitmapDrawable)loadImage( imageName );
            if( image != null ) {
                rounded = Images.toRoundedCorner( image, radius );
                ImageCache.put( cacheKey, rounded );
            }
        }
        return rounded;
    }
}