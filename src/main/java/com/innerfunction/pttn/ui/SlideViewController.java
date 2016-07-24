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
package com.innerfunction.pttn.ui;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.app.ViewController;

import static android.support.v4.view.GravityCompat.*;

/**
 * Attached by juliangoacher on 19/05/16.
 */
public class SlideViewController extends ViewController {

    private DrawerLayout drawerLayout;
    private int slidePosition = START;

    public SlideViewController(Context context) {
        super( context );
        setLayout("slide_view_layout");
    }

    @Override
    public View onCreateView(Activity activity) {
        drawerLayout = (DrawerLayout)super.onCreateView( activity );
        return drawerLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Ensure that the slide view is put back into a paused state after a restart, if the
        // drawer is hidden.
        if( !drawerLayout.isDrawerOpen( slidePosition ) ) {
            getSlideView().changeState( State.Paused );
        }
    }

    public void setSlideView(ViewController slideView) {
        removeChildViewController( getSlideView() );
        addChildViewController( slideView );
        layoutManager.setViewComponent("slide", slideView );
    }

    public ViewController getSlideView() {
        return (ViewController)layoutManager.getViewComponent("slide");
    }

    public void setMainView(ViewController mainView) {
        removeChildViewController( getMainView() );
        addChildViewController( mainView );
        layoutManager.setViewComponent("main", mainView );
    }

    public ViewController getMainView() {
        return (ViewController)layoutManager.getViewComponent("main");
    }

    public void setSlidePosition(String position) {
        slidePosition = "right".equals( position ) ? START : END;
    }

    public String getSlidePosition() {
        return slidePosition == START ? "right" : "left";
    }

    public void openDrawer() {
        getSlideView().changeState( getState() );
        drawerLayout.openDrawer( slidePosition );
    }

    public void closeDrawer() {
        drawerLayout.closeDrawers();
        getSlideView().changeState( State.Paused );
    }

    public void toggleDrawer() {
        if( drawerLayout.isDrawerOpen( slidePosition ) ) {
            closeDrawer();
        }
        else {
            openDrawer();
        }
    }

    @Override
    public boolean routeMessage(Message message, Object sender) {
        boolean routed = false;
        if( message.hasTarget("slide") ) {
            message = message.popTargetHead();
            ViewController slideView = getSlideView();
            if( message.hasEmptyTarget() ) {
                routed = slideView.receiveMessage( message, sender );
            }
            else {
                routed = slideView.routeMessage( message, sender );
            }
        }
        else if( message.hasTarget("main") ) {
            message = message.popTargetHead();
            ViewController mainView = getMainView();
            if( message.hasEmptyTarget() ) {
                routed = mainView.receiveMessage( message, sender );
            }
            else {
                routed = mainView.routeMessage( message, sender );
            }
            closeDrawer();
        }
        return routed;
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("show") ) {
            // Replace main view.
            setMainView( (ViewController)message.getParameter("view") );
            return true;
        }
        if( message.hasName("show-in-slide") ) {
            // Replace the slide view.
            setSlideView( (ViewController)message.getParameter( "view" ) );
            return true;
        }
        if( message.hasName("show-slide") ) {
            // Open the slide view.
            openDrawer();
            return true;
        }
        if( message.hasName("hide-slide") ) {
            // Close the slide view.
            closeDrawer();
            return true;
        }
        if( message.hasName("toggle-slide") ) {
            toggleDrawer();
        }
        return false;
    }
}
