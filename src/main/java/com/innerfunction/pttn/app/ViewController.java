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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.MessageReceiver;
import com.innerfunction.pttn.MessageRouter;
import com.innerfunction.util.KeyPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class for displaying and controlling PTTN views.
 * This class should be used in preference to Fragment (or its ViewFragment subclass) because of
 * well documented problems with using fragments, especially when nested within one another.
 * The ViewController class provides a simplified version of the Fragment lifecycle; a lot of the
 * Fragment lifecycle methods are provided with the same names, and the same or simplified
 * method signatures.
 *
 * TODO Add methods allowing nested controllers to control the title & nav bar buttons of the top
 * TODO -most controller; LayoutManager could be extended so that each nested view controller has
 * TODO a reference to its parent view controller.
 *
 * Attached by juliangoacher on 17/05/16.
 */
public class ViewController extends FrameLayout implements MessageReceiver, MessageRouter {

    static final String Tag = ViewController.class.getSimpleName();

    public enum State { Instantiated, Attached, Started, Running, Paused, Stopped, Destroyed }

    /** The view's current lifecycle state. */
    protected State state = State.Instantiated;
    /**
     * A flag indicating whether the view should be transitioned into the Running state.
     * This flag can be used by container views to control which of their child views should be
     * transitioned to Running when the parent view is transitioned to Running. It is typically
     * set to false on hidden views; for example, the navigation view controller sets the flag to
     * false on all views on the navigation stack _except_ for the top, visible view.
     */
    protected boolean runnable = true;
    /** The view's layout manager. */
    protected LayoutManager layoutManager;
    /** The activity the view is attached to. */
    private Activity activity;
    /** This view's title bar state. */
    protected VirtualTitleBar titleBarState = new VirtualTitleBar();
    /** The view's view - i.e. the thing it displays and controls. */
    private View view;
    /** The view controller's parent view controller, if any. */
    private ViewController parentViewController;
    /** A list of this controller's child view controllers. */
    private List<ViewController> childViewControllers = new ArrayList<>();
    /** Flag indicating whether to hide the view's title (i.e. action) bar. */
    private Boolean hideTitleBar;
    /** The view's title. */
    private String title;
    /** The view's background colour. */
    private int backgroundColor = Color.TRANSPARENT;
    /** A list of view behaviours. */
    private List<ViewControllerBehaviour> behaviours = new ArrayList<>();

    public ViewController(Context context) {
        super( context );
        this.layoutManager = new LayoutManager( context );
    }

    /** Get the view's current lifecycle state. */
    public State getState() {
        return state;
    }

    public void setRunnable(boolean runnable) {
        this.runnable = runnable;
    }

    /**
     * Change the view's current state.
     * This method contains the view's lifecycle FSM.
     *
     * @param newState
     */
    public void changeState(State newState) {
        switch( newState ) {
        case Instantiated:
            Log.e( Tag, "Can't transition to the Instantiated state");
            break;
        case Attached:
            if( state == State.Instantiated ) {
                for( ViewController child : childViewControllers ) {
                    child.changeState( State.Attached );
                }
                state = State.Attached;
            }
            else if( state != State.Attached ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        case Started:
            if( state == State.Attached || state == State.Stopped ) {
                for( ViewController child : childViewControllers ) {
                    child.changeState( State.Started );
                }
                onStart();
                state = State.Started;
            }
            else if( state != State.Started ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        case Running:
            if( state == State.Attached || state == State.Stopped ) {
                changeState( State.Started );
            }
            if( state == State.Started || state == State.Paused ) {
                for( ViewController child : childViewControllers ) {
                    if( child.runnable ) {
                        child.changeState( State.Running );
                    }
                }
                onResume();
                // Apply behaviours.
                for( ViewControllerBehaviour behaviour : behaviours ) {
                    behaviour.onResume( this );
                }
                state = State.Running;
            }
            else if( state != State.Running ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        case Paused:
            if( state == State.Attached ) {
                changeState( State.Started );
            }
            if( state == State.Started || state == State.Running ) {
                for( ViewController child : childViewControllers ) {
                    child.changeState( State.Paused );
                }
                onPause();
                state = State.Paused;
            }
            else if( state != State.Paused ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        case Stopped:
            if( state.ordinal() < State.Paused.ordinal() ) {
                changeState( State.Paused );
            }
            if( state == State.Paused ) {
                for( ViewController child : childViewControllers ) {
                    child.changeState( State.Stopped );
                }
                onStop();
                state = State.Stopped;
            }
            else if( state != State.Stopped ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        case Destroyed:
            if( state.ordinal() < State.Stopped.ordinal() ) {
                changeState( State.Stopped );
            }
            if( state == State.Stopped ) {
                for( ViewController child : childViewControllers ) {
                    child.changeState( State.Destroyed );
                }
                onDestroy();
                state = State.Destroyed;
            }
            else if( state != State.Destroyed ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        }
    }

    public void onAttach(Activity activity) {
        this.activity = activity;
        if( state == State.Instantiated ) {
            this.view = onCreateView( activity );
            for( ViewController child : childViewControllers ) {
                child.onAttach( activity );
            }
            changeState( State.Attached );
        }
        else {
            // If the view controller is stopped and detached from the window, and then
            // subsequently reused then its view won't be null at this point.
            // This can happen with the root view when the app is exited and restarted.
            assert this.state == State.Stopped;
            assert this.view != null;
        }
    }

    public void setTitleBar(TitleBar titleBar) {
        titleBarState.setTitleBar( titleBar );
    }

    public TitleBar getTitleBar() {
        return titleBarState.getTitleBar();
    }

    public TitleBarState getTitleBarState() {
        return titleBarState;
    }

    public View onCreateView(Activity activity) {
        // If the fragment has a layout setting then inflate it and extract any view components.
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = layoutManager.inflate( inflater, this );
        if( view != null ) {
            if( backgroundColor != Color.TRANSPARENT ) {
                view.setBackgroundColor( backgroundColor );
            }
            // Add the view as (the sole) child of the view controller.
            FrameLayout.LayoutParams layoutParams
                = new FrameLayout.LayoutParams( FrameLayout.LayoutParams.MATCH_PARENT,
                                                FrameLayout.LayoutParams.MATCH_PARENT );
            view.setLayoutParams( layoutParams );
            removeAllViewsInLayout();
            addView( view );
        }
        return view;
    }

    public ViewController getParentViewController() {
        return parentViewController;
    }

    protected void addChildViewController(ViewController child) {
        if( !childViewControllers.contains( child ) ) {
            childViewControllers.add( child );
            child.parentViewController = this;
            if( activity != null ) {
                child.onAttach( activity );
            }
            // Make the child forward title bar state updates through this view's title bar state.
            child.setTitleBar( titleBarState );
        }
    }

    protected void removeChildViewController(ViewController child) {
        if( child != null ) {
            child.changeState( State.Stopped );
            childViewControllers.remove( child );
            child.parentViewController = null;
        }
    }

    protected List<ViewController> getChildViewControllers() {
        return childViewControllers;
    }

    @Override
    public void onDetachedFromWindow() {
        parentViewController = null;
        changeState( State.Destroyed );
        super.onDetachedFromWindow();
    }

    public void onStart() {}

    public void onResume() {
        titleBarState.apply();
    }

    public void onPause() {}

    public void onStop() {}

    public void onDestroy() {}

    /**
     * Notify the view of a back button press.
     * @return true if the button should be processed as normal; false if the view has processed
     *         the button press.
     */
    public boolean onBackPressed() {
        return true;
    }

    public void postMessage(String message) {
        AppContainer.getAppContainer().postMessage( message, this );
    }

    public void showToast(String message) {
        Toast.makeText( getActivity(), message, Toast.LENGTH_LONG ).show();
    }

    @Override
    public boolean receiveMessage(Message message, Object sender) {
        // First try passing message to behaviours.
        for( ViewControllerBehaviour behaviour : behaviours ) {
            if( behaviour.receiveMessage( message, sender ) ) {
                return true;
            }
        }
        // Then check for standard messages.
        if( message.hasName("toast") ) {
            String toastMessage = (String)message.getParameter("message");
            if( toastMessage != null ) {
                showToast( toastMessage );
            }
            return true;
        }
        if( message.hasName("dismiss-modal") ) {
            if( activity instanceof ViewControllerActivity ) {
                ((ViewControllerActivity)activity).dismissModalView();
                return true;
            }
        }
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
                // Try introspecting on current object for property with targetName.
                targetView = KeyPath.resolve( targetName, this );
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

    public void showModalView(ViewController view) {
        if( activity instanceof ViewControllerActivity ) {
            ((ViewControllerActivity)activity).showModalView( view );
        }
        else Log.w( Tag, "Unable to show modal view, not attached to instance of ViewControllerActivity");
    }

    public void dismissModalView() {
        if( activity instanceof ViewControllerActivity ) {
            ((ViewControllerActivity)activity).dismissModalView();
        }
    }

    // Properties

    public void setTitle(String title) {
        this.title = title;
        titleBarState.setTitle( title );
    }

    public String getTitle() {
        return title;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        super.setBackgroundColor( backgroundColor );
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setHideTitleBar(boolean hideTitleBar) {
        this.titleBarState.setTitleBarHidden( hideTitleBar );
    }

    public void setTitleBarColor(int color) {
        this.titleBarState.setTitleBarColor( color );
    }

    public void setTitleBarTextColor(int color) {
        this.titleBarState.setTitleBarTextColor( color );
    }

    public void setLeftTitleBarButton(TitleBarButton button) {
        button.setOwner( this );
        this.titleBarState.setLeftTitleBarButton( button );
    }

    public void setRightTitleBarButton(TitleBarButton button) {
        button.setOwner( this );
        this.titleBarState.setRightTitleBarButton( button );
    }

    public void setLayoutName(String layoutName) {
        layoutManager.setLayoutName( layoutName );
    }

    public String getLayoutName() {
        return layoutManager.getLayoutName();
    }

    public void setViewComponents(Map<String,Object> components) {
        for( Object component : components.values() ) {
            if( component instanceof ViewController ) {
                addChildViewController( (ViewController)component );
            }
        }
        layoutManager.setViewComponents( components );
    }

    public Map<String,Object> getViewComponents() {
        return layoutManager.getViewComponents();
    }

    public void addViewComponent(String name, Object component) {
        if( component instanceof ViewController ) {
            addChildViewController( (ViewController)component );
        }
        layoutManager.addViewComponent( name, component );
    }

    public Activity getActivity() {
        return activity;
    }

    public void setBehaviours(List<ViewControllerBehaviour> behaviours) {
        this.behaviours = behaviours;
    }

    public void setBehaviour(ViewControllerBehaviour behaviour) {
        if( behaviour != null ) {
            this.behaviours.add( behaviour );
        }
    }

    public void addBehaviour(ViewControllerBehaviour behaviour) {
        if( behaviour != null ) {
            this.behaviours.add( behaviour );
        }
    }
}
