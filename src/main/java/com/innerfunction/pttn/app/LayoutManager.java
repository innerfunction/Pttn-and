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
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * Attached by juliangoacher on 28/04/16.
 */
public class LayoutManager {

    static final String Tag = LayoutManager.class.getSimpleName();

    private Fragment fragment;
    private Context context;
    private Map<String,Object> viewComponents = new HashMap<>();
    private String layoutName;
    private View layout;
    /**
     * The view ID of the title bar delegate.
     * The title bar  delegate is a view controller within the layout which is given control
     * over the app's title bar. This method is useful in compound views, e.g. slide view
     * controller, where just a single child view controls things like the title text.
     */
    private String titleBarDelegateViewID;
    private TitleBar titleBar;

    public LayoutManager(Fragment fragment) {
        this.fragment = fragment;
    }

    public LayoutManager(Context context) {
        this.context = context;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public void setTitleBarDelegateViewID(String viewID) {
        this.titleBarDelegateViewID = viewID;
        refreshTitleBarDelegate();
    }

    public void setTitleBar(TitleBar titleBar) {
        this.titleBar = titleBar;
        refreshTitleBarDelegate();
    }

    private void refreshTitleBarDelegate() {
        if( this.titleBarDelegateViewID != null && this.titleBar != null ) {
            Object delegate = viewComponents.get( titleBarDelegateViewID );
            if( delegate instanceof ViewController ) {
                ((ViewController)delegate).setTitleBar( titleBar );
            }
        }
    }

    private Context getContext() {
        if( context == null && fragment != null ) {
            context = fragment.getActivity();
        }
        if( context == null ) {
            throw new RuntimeException("LayoutManager: Unable to resolve context");
        }
        return context;
    }

    private Resources getResources() {
        return getContext().getResources();
    }

    private String getPackageName() {
        return getContext().getPackageName();
    }

    private int getLayoutID() {
        int layoutID = 0;
        if( layoutName != null ) {
            Resources r = getResources();
            String packageName = getPackageName();
            layoutID = r.getIdentifier( layoutName, "layout", packageName );
        }
        return layoutID;
    }

    public void setViewComponents(Map<String,Object> viewComponents) {
        this.viewComponents = viewComponents;
        refreshTitleBarDelegate();
    }

    public void addViewComponent(String name, Object component) {
        viewComponents.put( name, component );
    }

    public Map<String,Object> getViewComponents() {
        return viewComponents;
    }

    public void setViewComponent(String name, Object component) {
        viewComponents.put( name, component );
        if( layout != null ) {
            populateViewComponent( name, component );
        }
        refreshTitleBarDelegate();
    }

    public Object getViewComponent(String name) {
        return viewComponents.get( name );
    }

    public void destroy() {
        fragment = null;
        context = null;
        viewComponents = null;
    }

    public View inflate(LayoutInflater inflater, ViewGroup container) {
        layout = null;
        int layoutID = getLayoutID();
        if( layoutID > 0 ) {
            layout = inflater.inflate( layoutID, container, false );
            populateViewComponents();
        }
        return layout;
    }

    public View inflate(ViewGroup container) {
        layout = null;
        int layoutID = getLayoutID();
        if( layoutID > 0 ) {
            layout = View.inflate( context, layoutID, container );
            populateViewComponents();
        }
        return layout;
    }

    public void populateViewComponents() {
        // Iterate over the layout's component views...
        for( String name : viewComponents.keySet() ) {
            Object component = viewComponents.get( name );
            populateViewComponent( name, component );
        }
    }

    public void populateViewComponent(String name, Object component) {
        if( component instanceof View ) {
            replaceView( name, (View)component );
        }
        else if( component instanceof Fragment ) {
            FragmentTransaction transaction = fragment.getChildFragmentManager().beginTransaction();
            Fragment childFragment = (Fragment)component;
            // Apparently it is Major Error with deadly repercussions causing immediate
            // app destruction to have retainInstance = true on a nested fragment, so
            // ensure it is set to false. (Seriously - why can't Android just ignore it
            // in this case?)
            childFragment.setRetainInstance( false );
            // The code here is based on http://xperiment-andro.blogspot.ie/2013/02/nested-fragments.html
            // 1. Instantiate a FrameLayout to hold the fragment.
            // 2. Replace the current view in the layout with the frame layout.
            // 3. Add the fragment to the frame layout.
            View frame = new FrameLayout( context );
            if( replaceView( name, frame ) ) {
                transaction.replace( frame.getId(), childFragment, name );
            }
            transaction.commit();
        }
        else {
            Log.w( Tag, String.format( "Layout view %s is not an instance of View or Fragment", name ) );
        }
    }

    public boolean replaceView(String viewID, View newView) {
        boolean ok = false;
        FrameLayout frame = getViewFrame( viewID );
        if( frame != null ) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            );
            newView.setLayoutParams( layoutParams );
            frame.removeAllViews();
            frame.addView( newView );
            ok = true;
        }
        else {
            Log.w( Tag, String.format("View frame with ID %s not found in layout %s", viewID, layoutName ) );
        }
        return ok;
    }

    public View getView(String viewID) {
        if( layout == null ) {
            return null;
        }
        // Find the resource ID of the view.
        Resources r = getResources();
        String packageName = getPackageName();
        int id = r.getIdentifier( viewID, "id", packageName );
        // Find the view in the layout.
        return layout.findViewById( id );
    }

    protected FrameLayout getViewFrame(String viewID) {
        View view = getView( viewID );
        if( view instanceof FrameLayout ) {
            return (FrameLayout)view;
        }
        return null;
    }

}
