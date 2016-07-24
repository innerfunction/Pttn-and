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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.MessageReceiver;
import com.innerfunction.pttn.MessageRouter;

import java.util.Map;

/**
 * Attached by juliangoacher on 28/04/16.
 */
public class ViewFragment extends Fragment implements ActivityViewFragment, MessageReceiver, MessageRouter {

    protected LayoutManager layoutManager = new LayoutManager( this );
    /** Flag indicating whether to hide the view's title (i.e. action) bar. */
    private boolean hideTitleBar;
    /** The view's title. */
    private String title;
    /** The view's background colour. */
    private int backgroundColor;

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setLayout(String layout) {
        layoutManager.setLayoutName( layout );
    }

    public String getLayout() {
        return layoutManager.getLayoutName();
    }

    public void setViewComponents(Map<String,Object> components) {
        layoutManager.setViewComponents( components );
    }

    public Map<String,Object> getViewComponents() {
        return layoutManager.getViewComponents();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //View view = super.onCreateView( inflater, container, savedInstanceState );
        // If the fragment has a layout setting then inflate it and extract any view components.
        View view = layoutManager.inflate( inflater, container );
        view.setBackgroundColor( backgroundColor );
        return view;
    }

    @Override
    public boolean onBackPressed() {
        return true;
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        // iOS code iterates over behaviours; and then checks for "toast" and "show-image" messages.
        return false;
    }

    @Override
    public boolean routeMessage(Message message, Object sender) {
        boolean routed = false;
        Object targetView = null;
        String targetName = message.targetHead();
        if( targetName != null ) {
            targetView = getViewComponents().get( targetName );
            if( targetView == null ) {
                // TODO: Try introspecting on current object for property with targetName
            }
            if( targetView != null ) {
                message = message.popTargetHead();
                if( message.hasEmptyTarget() ) {
                    if( targetView instanceof MessageReceiver ) {
                        routed = ((MessageReceiver)targetView).receiveMessage( message, sender );
                    }
                }
                else if( targetView instanceof MessageRouter ) {
                    routed = ((MessageRouter)targetView).routeMessage( message, sender );
                }
            }
        }
        return routed;
    }

}
