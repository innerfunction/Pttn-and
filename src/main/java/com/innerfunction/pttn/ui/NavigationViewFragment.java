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
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.R;
import com.innerfunction.pttn.app.ViewFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by juliangoacher on 04/05/16.
 * @deprecated
 */
public class NavigationViewFragment extends ViewFragment {

    static final String Tag = NavigationViewFragment.class.getSimpleName();

    /** Thin extension of ArrayList providing stack type API calls. */
    static class ViewStack extends ArrayList<Fragment> {
        /** Push a new fragment onto the stack. */
        public void push(Fragment view) {
            add( view );
        }
        /** Pop the top view from the stack. */
        public Fragment pop() {
            int s = size();
            if( s > 0 ) {
                return remove( s - 1 );
            }
            return null;
        }
        /** Get the root view. */
        public Fragment getRootView() {
            return size() > 0 ? get( 0 ) : null;
        }
        /** Get the top view. */
        public Fragment getTopView() {
            int s = size();
            return s > 0 ? get( s - 1 ) : null;
        }
        /** Trim the stack to the specified size. */
        public void trim(int size) {
            while( size() > size ) {
                remove( size() - 1 );
            }
        }
    }

    /**
     * A stack of the navigated views.
     * The top of the stack is the currently visible view. The second item on the stack is the
     * previously visible view, and so on. Pressing the back button will navigate back by popping
     * items from the stack. There will always be at least one item on the stack, assuming at least
     * one item as initially added.
     */
    private ViewStack views = new ViewStack();

    public NavigationViewFragment() {
        setLayout("view_acivity_layout");
        getFragmentManager().addOnBackStackChangedListener( new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                // When back button is pushed, ensure that number of views matches size of stack.
                FragmentManager fm = getFragmentManager();
                views.trim( fm.getBackStackEntryCount() );
            }
        } );
    }

    public void setRootView(Fragment view) {
        setViews( Arrays.asList( view ) );
    }

    public Fragment getRootView() {
        return views.getRootView();
    }

    public void setViews(List<Fragment> views) {
        this.views.clear();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        for( int i = 0; i < views.size(); i++ ) {
            Fragment view = views.get( i );
            ft.add( R.id.main, view );
            ft.addToBackStack( i == 0 ? "root" : null );
        }
        ft.commit();
        this.views.addAll( views );
    }

    public void pushView(Fragment view) {
        Fragment currentView = views.getTopView();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add( R.id.main, view );
        // currentView.onPause(); TODO is this call needed?
        ft.hide( currentView );
        ft.addToBackStack( null );
        ft.commit();
        views.push( view );
    }

    public Fragment popView() {
        Fragment poppedView = null;
        if( views.size() > 1 ) {
            poppedView = views.pop();
            FragmentManager fm = getFragmentManager();
            fm.popBackStack();
        }
        return poppedView;
    }

    public boolean popToRootView() {
        boolean popped = false;
        FragmentManager fm = getFragmentManager();
        if( fm.getBackStackEntryCount() > 1 ) {
            fm.popBackStack("root", 0 );
            popped = true;
        }
        return popped;
    }

    @Override
    public boolean onBackPressed() {
        // Tell the activity to continue normal back button behaviour if nothing was popped here.
        return popView() == null;
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        if( message.hasName("show") ) {
            Object view = message.getParameter("view");
            if( view instanceof Fragment ) {
                if("reset".equals( message.getParameter("navigation") ) ) {
                    setViews( Arrays.asList( (Fragment)view ) );
                }
                else {
                    pushView( (Fragment)view );
                }
            }
            else if( view != null ) {
                Log.w( Tag, String.format("Unable to show view of type %s", view.getClass() ) );
            }
            else {
                Log.w( Tag, "Unable to show null view");
            }
            return true;
        }
        else if( message.hasName("back") ) {
            popView();
            return true;
        }
        else if( message.hasName("home") ) {
            popToRootView();
            return true;
        }
        return false;
    }

}
