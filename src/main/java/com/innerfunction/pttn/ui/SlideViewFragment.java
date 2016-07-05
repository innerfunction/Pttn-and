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

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.MessageReceiver;
import com.innerfunction.pttn.MessageRouter;
import com.innerfunction.pttn.app.ViewFragment;

import static android.support.v4.view.GravityCompat.*;

/**
 * Created by juliangoacher on 28/04/16.
 * @deprecated
 */
public class SlideViewFragment extends ViewFragment {

    private DrawerLayout drawerLayout;
    private int slidePosition = START;

    public SlideViewFragment() {
        setLayout("slide_view_layout");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        drawerLayout = (DrawerLayout)super.onCreateView( inflater, container, savedInstanceState );
        return drawerLayout;
    }

    public void setSlideView(Fragment slideView) {
        layoutManager.setViewComponent("slide", slideView );
    }

    public Fragment getSlideView() {
        return (Fragment)layoutManager.getViewComponent("slide");
    }

    public void setMainView(Fragment mainView) {
        layoutManager.setViewComponent("main", mainView );
    }

    public Fragment getMainView() {
        return (Fragment)layoutManager.getViewComponent("main");
    }

    public void setSlidePosition(String position) {
        slidePosition = "right".equals( position ) ? START : END;
    }

    public String getSlidePosition() {
        return slidePosition == START ? "right" : "left";
    }

    @Override
    public boolean routeMessage(Message message, Object sender) {
        boolean routed = false;
        if( message.hasTarget("slide") ) {
            message = message.popTargetHead();
            Fragment slideView = getSlideView();
            if( message.hasEmptyTarget() && slideView instanceof MessageReceiver ) {
                routed = ((MessageReceiver)slideView).receiveMessage( message, sender );
            }
            else if( slideView instanceof MessageRouter ) {
                routed = ((MessageRouter)slideView).routeMessage( message, sender );
            }
        }
        else if( message.hasTarget("main") ) {
            message = message.popTargetHead();
            Fragment mainView = getMainView();
            if( message.hasEmptyTarget() && mainView instanceof MessageReceiver ) {
                routed = ((MessageReceiver)mainView).receiveMessage( message, sender );
            }
            else if( mainView instanceof MessageRouter ) {
                routed = ((MessageRouter)mainView).routeMessage( message, sender );
            }
            drawerLayout.closeDrawers();
        }
        return routed;
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("show") ) {
            // Replace main view.
            setMainView( (Fragment)message.getParameter("view") );
            return true;
        }
        if( message.hasName("show-in-slide") ) {
            // Replace the slide view.
            setSlideView( (Fragment)message.getParameter( "view" ) );
            return true;
        }
        if( message.hasName("show-slide") ) {
            // Open the slide view.
            drawerLayout.openDrawer( slidePosition );
            return true;
        }
        if( message.hasName("hide-slide") ) {
            // Close the slide view.
            drawerLayout.closeDrawers();
            return true;
        }
        if( message.hasName("toggle-slide") ) {
            if( drawerLayout.isDrawerOpen( slidePosition ) ) {
                drawerLayout.closeDrawers();
            }
            else {
                drawerLayout.openDrawer( slidePosition );
            }
        }
        return false;
    }
}
