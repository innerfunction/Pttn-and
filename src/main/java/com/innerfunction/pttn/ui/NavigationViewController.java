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
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.app.Chrome;
import com.innerfunction.pttn.app.ViewController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Attached by juliangoacher on 19/05/16.
 */
public class NavigationViewController extends ViewController {

    static final String Tag = NavigationViewController.class.getSimpleName();

    /** Thin extension of ArrayList providing stack type API calls. */
    static class ViewStack extends ArrayList<ViewController> {

        /** Push a new fragment onto the stack. */
        public void push(ViewController view) {
            add( view );
        }

        /**
         * Pop the top view from the stack.
         * Returns the new top view, i.e. the second last item on the stack before calling this
         * method; or null if the stack is empty.
         */
        public ViewController pop() {
            int s = size();
            if( s > 0 ) {
                remove( s - 1 );
                return getTopView();
            }
            return null;
        }

        /** Get the root view. */
        public ViewController getRootView() {
            return size() > 0 ? get( 0 ) : null;
        }

        /** Get the top view. */
        public ViewController getTopView() {
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

    /** Standard navigate forward transition. */
    static Transition NavigateForwardTransition;
    /** Standard navigate back transition. */
    static Transition NavigateBackTransition;

    static {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            NavigateForwardTransition = new Slide( Gravity.RIGHT );
            NavigateBackTransition = new Slide( Gravity.LEFT );
        }
    }

    /** The main layout. */
    private ViewGroup layout;
    /**
     * A stack of the navigated views.
     * The top of the stack is the currently visible view. The second item on the stack is the
     * previously visible view, and so on. Pressing the back button will navigate back by popping
     * items from the stack. There will always be at least one item on the stack, assuming at least
     * one item as initially added.
     */
    private ViewStack views = new ViewStack();
    /** The top-most view. */
    private ViewController topView;

    public NavigationViewController(Context context) {
        super( context );
        setLayoutName("navigation_view_layout");
    }

    @Override
    public View onCreateView(Activity activity) {
        this.layout = (ViewGroup)super.onCreateView( activity );
        for( ViewController child : getChildViewControllers() ) {
            layout.addView( child );
        }
        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Ensure that all non-visible views are put back into a paused state after a restart.
        for( int i = 0; i < views.size() - 2; i++ ) {
            views.get( i ).changeState( State.Paused );
        }
    }

    @Override
    public void setChrome(Chrome chrome) {
        super.setChrome( chrome );
        // This view instance's chrome configuration is disabled; instead, control over the chrome
        // is delegated to all view controllers on the navigation stack; actual control resides
        // with the running - i.e. the top - view.
        for( ViewController view : views ) {
            view.setChrome( chrome );
        }
    }

    public void setRootView(ViewController view) {
        setViews( Arrays.asList( view ) );
    }

    public ViewController getRootView() {
        return views.getRootView();
    }

    public void setViews(List<ViewController> newViews) {
        // Want to preserve any views currently on the stack which are in the same position in the
        // new list.
        // First, find the position of the first view in the current list that doesn't match the
        // view in the same position in the new list.
        int i;
        for( i = 0; i < views.size(); i++ ) {
            if( i > newViews.size() ) {
                break;
            }
            if( views.get( i ) != newViews.get( i ) ) {
                break;
            }
        }
        // Next, remove all views on the current stack which are beyond the last common position.
        for( int j = views.size() - 1; j >= i; j-- ) {
            removeChildViewController( views.get( j ) );
        }
        // Trim the current view list to the correct size.
        views.trim( i );
        // Add all new views to the controller and stack.
        for( i = i; i < newViews.size(); i++ ) {
            ViewController newView = newViews.get( i );
            addChildViewController( newView );
            views.push( newView );
        }
        // Ensure that the top view matches this view's state.
        topView = views.getTopView();
        State state = getState();
        if( state != State.Instantiated && state != State.Destroyed ) {
            topView.changeState( getState() );
        }
    }

    public void pushView(ViewController newView) {
        // Transition to the new view.
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            TransitionManager.beginDelayedTransition( layout, NavigateForwardTransition );
        }
        // Pause the current top view.
        topView.changeState( State.Paused );
        // Add the new view and change to current state.
        addChildViewController( newView );
        newView.changeState( getState() );
        // Update stack.
        views.push( newView );
        topView = newView;
    }

    public ViewController popView() {
        ViewController poppedView = null;
        int viewCount = views.size();
        if( viewCount > 1 ) {
            // Remove the current top view.
            poppedView = topView;
            poppedView.changeState( State.Paused );
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                TransitionManager.beginDelayedTransition( layout, NavigateBackTransition );
            }
            removeChildViewController( poppedView );
            // Resume the next view.
            topView = views.pop();
            topView.changeState( getState() );
        }
        return poppedView;
    }

    public boolean popToRootView() {
        boolean popped = false;
        int viewCount = views.size();
        if( viewCount > 1 ) {
            // Start a back transition.
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                TransitionManager.beginDelayedTransition( layout, NavigateBackTransition );
            }
            // Remove and destroy all views on the stack, except the first view.
            // Note that views are removed in the reverse of the order they were added to the stack.
            for( int i = viewCount - 1; i > 0; i-- ) {
                ViewController view = views.get( i );
                view.changeState( State.Paused );
                removeChildViewController( view );
            }
            // The root view is now the top view...
            topView = views.getTopView();
            topView.changeState( getState() );
            // Remove discarded items from the navigation stack.
            views.trim( 1 );
            popped = true;
        }
        return popped;
    }

    @Override
    public void addChildViewController(ViewController child) {
        super.addChildViewController( child );
        if( layout != null ) {
            layout.addView( child );
        }
        if( chrome != null ) {
            child.setChrome( chrome );
        }
    }

    @Override
    public void removeChildViewController(ViewController child) {
        super.removeChildViewController( child );
        if( layout != null ) {
            layout.removeView( child );
        }
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
            if( view instanceof ViewController ) {
                if( "reset".equals( message.getParameter("navigation") ) ) {
                    setViews( Arrays.asList( views.get( 0 ), (ViewController)view ) );
                }
                else {
                    pushView( (ViewController)view );
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
