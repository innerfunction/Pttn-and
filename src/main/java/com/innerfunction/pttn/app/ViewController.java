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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.innerfunction.pttn.Message;
import com.innerfunction.pttn.MessageReceiver;
import com.innerfunction.pttn.MessageRouter;

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
 * Created by juliangoacher on 17/05/16.
 */
public class ViewController extends FrameLayout implements MessageReceiver, MessageRouter {

    static final String Tag = ViewController.class.getSimpleName();

    public enum State { Instantiated, Created, Started, Running, Paused, Stopped, Destroyed }

    /** The view's current lifecycle state. */
    protected State state = State.Instantiated;
    /** The view's layout manager. */
    protected LayoutManager layoutManager;
    /** The activity the view is attached to. */
    private Activity activity;
    /** The view's view - i.e. the thing it displays and controls. */
    private View view;
    /** The view controller's parent view controller, if any. */
    private ViewController parentViewController;
    /** A list of this controller's child view controllers. */
    private List<ViewController> childViewControllers = new ArrayList<>();
    /** Flag indicating whether to hide the view's title (i.e. action) bar. */
    private boolean hideTitleBar;
    /** The view's title. */
    private String title;
    /** The view's background colour. */
    private int backgroundColor = -1;

    public ViewController(Context context) {
        super( context );
        this.layoutManager = new LayoutManager( context );
    }

    /** Get the view's current lifecycle state. */
    public State getState() {
        return state;
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
        case Created:
            if( state == State.Instantiated ) {
                state = State.Created;
            }
            else if( state != State.Created ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        case Started:
            if( state == State.Created || state == State.Stopped ) {
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
            if( state == State.Created || state == State.Stopped ) {
                changeState( State.Started );
            }
            if( state == State.Started || state == State.Paused ) {
                for( ViewController child : childViewControllers ) {
                    child.changeState( State.Running );
                }
                onResume();
                state = State.Running;
            }
            else if( state != State.Running ) {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        case Paused:
            if( state == State.Running ) {
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
            if( state == State.Running ) {
                changeState( State.Paused );
            }
            if( state == State.Started || state == State.Paused ) {
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
            if( state != State.Destroyed && state != State.Stopped ) {
                changeState( State.Stopped );
            }
            if( state == State.Stopped ) {
                for( ViewController child : childViewControllers ) {
                    child.changeState( State.Destroyed );
                }
                onDestroy();
                state = State.Destroyed;
            }
            else {
                Log.e( Tag, String.format("Illegal state change: %s -> %s", state, newState ) );
            }
            break;
        }
    }

    public void onAttach(Activity activity) {
        this.activity = activity;
        this.view = onCreateView( activity );
        // TODO Should this only apply for the top-most view controller?
        // TODO If so, is this the bast place to put the code?
        if( hideTitleBar ) {
            activity.getActionBar().hide();
        }
        changeState( State.Created );
    }

    public View onCreateView(Activity activity) {
        // If the fragment has a layout setting then inflate it and extract any view components.
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = layoutManager.inflate( inflater, this );
        if( view != null && backgroundColor > -1 ) {
            view.setBackgroundColor( backgroundColor );
        }
        return view;
    }

    public ViewController getParentViewController() {
        return parentViewController;
    }

    protected void addChildViewController(ViewController child) {
        if( !childViewControllers.contains( child ) ) {
            childViewControllers.add( child );
        }
    }

    protected void removeChildViewController(ViewController child) {
        childViewControllers.remove( child );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // See if a view controller parent exists.
        ViewParent parent = this;
        while( (parent = parent.getParent()) != null ) {
            if( parent instanceof ViewController ) {
                this.parentViewController = (ViewController)parent;
                parentViewController.addChildViewController( this );
                break;
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        changeState( State.Destroyed );
        if( parentViewController != null ) {
            parentViewController.removeChildViewController( this );
        }
        parentViewController = null;
    }

    public void onStart() {
        // Call the title setter so that the action bar is updated.
        setTitle( this.title );
    }

    public void onResume() {}

    public void onPause() {}

    public void onStop() {}

    public void onDestroy() {}

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

    // Properties

    public void setTitle(String title) {
        this.title = title;
        if( title != null ) {
            activity.getActionBar().setTitle( title );
        }
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

    public Activity getActivity() {
        return activity;
    }
}
