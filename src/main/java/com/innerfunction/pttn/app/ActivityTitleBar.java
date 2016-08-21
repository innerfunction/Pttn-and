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
package com.innerfunction.pttn.app;

import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * An activity's title bar.
 * Encapsulates functionality on the activity's toolbar and action bar instances.
 * Created by juliangoacher on 19/08/16.
 */
public class ActivityTitleBar implements TitleBar {

    private Toolbar toolbar;
    private ActionBar actionBar;

    public ActivityTitleBar(Toolbar toolbar, ActionBar actionBar) {
        this.toolbar = toolbar;
        this.actionBar = actionBar;
    }

    @Override
    public void applyState(TitleBarState state) {
        boolean hidden = state.getTitleBarHidden();
        if( hidden ) {
            actionBar.hide();
        }
        else {
            actionBar.show();
        }
        String title = state.getTitle();
        if( title != null ) {
            toolbar.setTitle( title );
        }
        int color = state.getTitleBarColor();
        if( color != 0 ) {
            toolbar.setBackgroundColor( color );
        }
        int textColor = state.getTitleBarTextColor();
        if( textColor != 0 ) {
            toolbar.setTitleTextColor( textColor );
        }
        final TitleBarButton leftButton = state.getLeftTitleBarButton();
        if( leftButton != null ) {
            actionBar.setHomeButtonEnabled( true );
            actionBar.setDisplayUseLogoEnabled( true );
            toolbar.setNavigationIcon( leftButton.getImage() );
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String action = leftButton.getAction();
                    ViewController viewController = leftButton.getOwner();
                    viewController.postMessage( action );
                }
            });
        }
        else {
            actionBar.setHomeButtonEnabled( false );
            actionBar.setDisplayUseLogoEnabled( false );
            toolbar.setNavigationIcon( null );
        }
        final TitleBarButton rightButton = state.getRightTitleBarButton();
        Menu menu = toolbar.getMenu();
        menu.clear();
        if( rightButton != null ) {
            MenuItem menuItem = menu.add( Menu.NONE, Menu.NONE, Menu.NONE, rightButton.getTitle() );
            Drawable image = rightButton.getImage();
            if( image != null ) {
                menuItem.setIcon( image );
                menuItem.setShowAsAction( MenuItem.SHOW_AS_ACTION_ALWAYS );
                menuItem.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String action = rightButton.getAction();
                        ViewController viewController = rightButton.getOwner();
                        viewController.postMessage( action );
                        return false;
                    }
                });
            }
        }
    }
}
