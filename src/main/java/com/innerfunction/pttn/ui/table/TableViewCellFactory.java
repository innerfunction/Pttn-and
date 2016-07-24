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

import android.annotation.SuppressLint;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;

import com.innerfunction.pttn.IOCContextAware;
import com.innerfunction.util.ValueMap;

import com.nakardo.atableview.foundation.NSIndexPath;
import com.nakardo.atableview.internal.ATableViewCellContainerView;
import com.nakardo.atableview.internal.ATableViewCellAccessoryView.ATableViewCellAccessoryType;
import com.nakardo.atableview.protocol.ATableViewDataSource;
import com.nakardo.atableview.view.ATableView;
import com.nakardo.atableview.view.ATableViewCell;
import com.nakardo.atableview.view.ATableViewCell.ATableViewCellStyle;

/**
 * A factory class for generating table view cell instances.
 *
 * Attached by juliangoacher on 05/05/16.
 */
public class TableViewCellFactory implements IOCContextAware {

    static final String Tag = TableViewCellFactory.class.getSimpleName();

    static final int DefaultRowHeight = 58;
    static final int DefaultImageHeight = 50;
    static final int DefaultImageWidth = 50;
    static final String BackgroundImageViewTag = "BackgroundImageView";

    public interface CellDecorator {
        ATableViewCell decorateCell(ATableViewCell cell, ValueMap data, TableViewCellFactory factory);
    }

    private TableViewController parent;
    private TableData tableData;

    private CellDecorator decorator;

    /** The cell display style. */
    private String style = "Style1";
    /** The default main text colour. */
    private int textColor = Color.BLACK;
    /** The default text colour for a selected cell. */
    private int selectedTextColor = Color.BLACK;
    /** The default detail text colour. */
    private int detailTextColor = Color.BLACK;
    /** The default detail text colour for a selected cell. */
    private int selectedDetailTextColor = Color.BLACK;
    /** The cell's default background colour. */
    private int backgroundColor = Color.WHITE;
    /** The default background colour for a selected cell. */
    @SuppressWarnings("unused")
    private int selectedBackgroundColor = Color.WHITE;
    /** The default cell height. */
    private Number height = DefaultRowHeight;
    /** The default width of the cell's image. */
    private Number imageHeight = DefaultImageHeight;
    /** The default height of the cell's image. */
    private Number imageWidth = DefaultImageWidth;
    /** The cell's default accessory style. */
    private String accessory;
    /** The cell's default image. */
    private Drawable image;
    /** A background image for the cell. */
    private Drawable backgroundImage;
    @SuppressWarnings("unused")
    /** A background image for a selected cell. */
    private Drawable selectedBackgroundImage;
    /**
     * The device pixel ratio.
     * For converting from logical to real pixels.
     */
    private float pixelRatio = 1;

    /** IOCContextAware interface. */
    @Override
    public void setAndroidContext(Context context) {
        this.pixelRatio = context.getResources().getDisplayMetrics().density;
    }

    public void setParent(TableViewController parent) {
        this.parent = parent;
    }

    public void setTableData(TableData tableData) {
        this.tableData = tableData;
    }

    public void setDecorator(CellDecorator decorator) {
        this.decorator = decorator;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setSelectedTextColor(int color) {
        this.selectedTextColor = color;
    }

    public void setDetailTextColor(int color) {
        this.detailTextColor = color;
    }

    public void setSelectedDetailTextColor(int color) {
        this.selectedDetailTextColor = color;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setSelectedBackgroundColor(int color) {
        this.selectedBackgroundColor = color;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setImageHeight(int height) {
        this.imageHeight = height;
    }

    public void setImageWidth(int width) {
        this.imageWidth = width;
    }

    public void setAccessory(String accessory) {
        this.accessory = accessory;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public void setBackgroundImage(Drawable image) {
        this.backgroundImage = image;
    }

    public void setSelectedBackgroundImage(Drawable image) {
        this.selectedBackgroundImage = image;
    }

    // A cell in iOS represents a table row in Android
    @SuppressLint("DefaultLocale")
    public ATableViewCell resolveCellForTable(ATableView tableView, NSIndexPath indexPath, ATableViewDataSource dataSource) {

        ValueMap rowData = tableData.getRowDataForIndexPath( indexPath );

        String style = rowData.getString("style", this.style );
        ATableViewCell cell = dataSource.dequeueReusableCellWithIdentifier( style );

        if( cell == null ) {
            ATableViewCellStyle cellStyle = ATableViewCellStyle.Default;
            if( style.equals("Style1") ) {
                cellStyle = ATableViewCellStyle.Value1;
            }
            else if( style.equals("Style2") ) {
                cellStyle = ATableViewCellStyle.Value2;
            }
            else if( style.equals("Subtitle") ) {
                cellStyle = ATableViewCellStyle.Subtitle;
            }
            cell = new ATableViewCell( cellStyle, style, parent.getActivity() );
        }

        // Process title and description
        String title = rowData.getString("title");
        String description = rowData.getString("description");

        TextView textLabel = cell.getTextLabel();
        textLabel.setText( title );
        textLabel.setTextColor( rowData.getColor("textColor", textColor ) );
        // TODO: android TextView only has setHighlightColor, not highlightedTextColor
        textLabel.setHighlightColor( rowData.getColor("selectedTextColor", selectedTextColor ) );
        textLabel.setBackgroundColor( Color.TRANSPARENT );

        TextView detailTextLabel = cell.getDetailTextLabel();
        if( description != null && detailTextLabel != null ) {
            detailTextLabel.setText( description );
            detailTextLabel.setTextColor( rowData.getColor("detailTextColor", detailTextColor ) );
            detailTextLabel.setHighlightColor( rowData.getColor("detailSelectedTextColor", selectedDetailTextColor ) );
            detailTextLabel.setBackgroundColor( Color.TRANSPARENT );
        }

        cell.setBackgroundColor( rowData.getColor("backgroundColor", backgroundColor ) );

        String imageName = rowData.getString("image");
        Drawable image = null;
        if( imageName != null ) {
            Number imageHeight = rowData.getNumber( "imageHeight", this.imageHeight );
            if( imageHeight == null ) {
                imageHeight = rowData.getNumber( "height", height );
            }
            if( imageHeight.intValue() == 0 ) {
                imageHeight = DefaultImageHeight;
            }
            Number imageWidth = rowData.getNumber( "imageWidth", this.imageWidth );
            if( imageWidth.intValue() == 0 ) {
                imageHeight = DefaultImageWidth;
            }

            float radius = imageHeight.floatValue() * pixelRatio * 4;
            image = tableData.loadRoundedImage( imageName, radius );
        }

        if( image != null ) {

            imageWidth = convertToRealPixels( imageWidth );
            imageHeight = convertToRealPixels( imageHeight );

            LayoutParams params = new ATableViewCellContainerView.LayoutParams( imageWidth.intValue(), imageHeight.intValue() );

            // Set margins and crop the image to fit the available space
            params.setMargins( 20, 0, 0, 0 );
            ImageView imageView = cell.getImageView();
            imageView.setLayoutParams( params );
            imageView.setScaleType( ScaleType.CENTER_CROP );
            imageView.setAdjustViewBounds( true );

            imageView.setImageDrawable( image );
        }
        else {
            cell.getImageView().setImageDrawable( null );
        }

        // Accessory
        String accessory = rowData.getString("accessory", this.accessory );
        if("None".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.None );
        }
        else if("DisclosureIndicator".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.DisclosureIndicator );
        }
        else if("DetailButton".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.DisclosureButton );
        }
        else if("Checkmark".equals( accessory ) ) {
            cell.setAccessoryType( ATableViewCellAccessoryType.Checkmark );
        }

        // Background image
        Drawable backgroundImage = tableData.loadImage( rowData.getString( "backgroundImage" ), this.backgroundImage );
        Drawable selectedImage = tableData.loadImage( rowData.getString( "selectedBackgroundImage" ), this.selectedBackgroundImage );

        StateListDrawable stateListDrawable = new StateListDrawable();
        if( backgroundImage != null ) {
            stateListDrawable.addState(new int[] { android.R.attr.state_enabled }, backgroundImage );
        }
        if( selectedImage != null ) {
            stateListDrawable.addState(new int[] { android.R.attr.state_selected }, selectedImage );
        }
        // NOTE: The following is a hack to get cell background images working correctly. This
        // implementation won't display the cell's selected state background image (or maybe it
        // does, but it hasn't been tested). Also, use of images bigger than the cell size may
        // cause problems. A proper implementation may require a subclass of ATableViewCell to
        // properly encapsulate the following logic and include state change (normal/selected)
        // logic.
        if( backgroundImage != null || selectedImage != null ) {
            ImageView backgroundView = (ImageView)cell.findViewWithTag( BackgroundImageViewTag );
            if( backgroundView == null ) {
                backgroundView = new ImageView( cell.getContext() );
                backgroundView.setTag( BackgroundImageViewTag );
                cell.addView( backgroundView );
                // Set top/bottom padding to avoid overwriting table cell separators.
                backgroundView.setPadding( 0, 2, 0, 2 );
            }
            backgroundView.setImageDrawable( stateListDrawable );
        }

        // Decorate the cell if a decorator is specified.
        if( decorator != null ) {
            decorator.decorateCell( cell, rowData, this );
        }

        return cell;
    }

    public Number heightForRowAtIndexPath(NSIndexPath indexPath) {
        ValueMap rowData = tableData.getRowDataForIndexPath( indexPath );
        return rowData.getNumber("height", this.height );
    }

    /**
     * Convert from logical pixels to real pixels
     */
    public float convertToRealPixels(Number logicalPixels) {
        return logicalPixels.intValue() * pixelRatio;
    }

}