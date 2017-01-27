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

import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.innerfunction.pttn.R;

/**
 * The default app container activity.
 * Used to display view controller instances.
 *
 * Attached by juliangoacher on 19/05/16.
 */
public class ViewControllerActivity extends PttnActivity<ViewController> {

    /**
     * The layout which contains the activity view.
     */
    private FrameLayout viewContainer;
    /**
     * The currently displayed view controller.
     */
    private ViewController activeViewController;
    /**
     * The main view controller being displayed by this activity.
     */
    private ViewController mainViewController;
    /**
     * The currently displayed modal view controller, if any.
     */
    private ViewController modalViewController;
    /**
     * The application title bar.
     */
    private TitleBar titleBar;

    /** A list of the different types of view transition. */
    enum ViewTransition { Replace, ShowModal, HideModal };

    @Override
    public void setContentView(int viewID) {
        super.setContentView( viewID );
        try {
            this.viewContainer = (FrameLayout)findViewById( R.id.view_container );
            if( viewContainer == null ) {
                Log.w(Tag, "R.id.view_container not found in activity layout");
            }
        }
        catch(ClassCastException e) {
            Log.w(Tag, "R.id.view_container in activity layout must be a FrameLayout instance");
        }
        try {
            Toolbar toolBar = (Toolbar)findViewById( R.id.titlebar );
            if( toolBar != null ) {
                setSupportActionBar( toolBar );
                ActionBar actionBar = getSupportActionBar();
                this.titleBar = new ActivityTitleBar( toolBar, actionBar );
            }
            else {
                Log.w(Tag, "R.id.titlebar not found in activity layout");
            }
        }
        catch(ClassCastException e) {
            Log.w(Tag, "R.id.titlebar in activity layout must be a Toolbar instance");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if( activeViewController != null ) {
            activeViewController.changeState( ViewController.State.Started );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if( activeViewController != null ) {
            activeViewController.changeState( ViewController.State.Running );
        }
    }

    @Override
    public void onPause() {
        if( activeViewController != null ) {
            activeViewController.changeState( ViewController.State.Paused );
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if( activeViewController != null ) {
            activeViewController.changeState( ViewController.State.Stopped );
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if( activeViewController != null ) {
            activeViewController.changeState( ViewController.State.Destroyed );
        }
        super.onDestroy();
        // Remove any child views. This is necessary because if the view has been supplied by
        // the app container (e.g. see AppContainer.showViewUsingActivityType) then it may
        // be reused at a later point, but an IllegalStateException will be thrown when the view
        // is added to the new activity if it hasn't been previously removed from the old one.
        viewContainer.removeAllViews();
    }

    @Override
    public void onBackPressed() {
        if( modalViewController != null ) {
            if( modalViewController.onBackPressed() ) {
                // Dismiss the modal if the modal view doesn't process the back button itself.
                dismissModalView();
            }
        }
        else if( activeViewController == null || activeViewController.onBackPressed() ) {
            super.onBackPressed();
        }
    }

    // TODO How does a modal affect the action bar?

    @Override
    public void showView(ViewController view) {
        // If a modal is visible then dismiss it.
        if( modalViewController != null ) {
            dismissModalView();
        }
        // If the view is already the main view then nothing else to do.
        if( mainViewController == view ) {
            return;
        }
        // Record the current view controller state, stop the current view.
        ViewController.State state = null;
        if( mainViewController != null ) {
            state = mainViewController.getState();
            mainViewController.changeState( ViewController.State.Stopped );
        }
        // Add the new view to the activity.
        view.onAttach( this );
        showView( view, ViewTransition.Replace );
        this.mainViewController = view;
        // Update the new view's state.
        if( state != null ) {
            mainViewController.changeState( state );
        }
    }

    public void showModalView(ViewController view) {
        // If a modal is already visible then dismiss it.
        if( modalViewController != null ) {
            dismissModalView();
        }
        // Record the current view state, pause the current view.
        mainViewController.changeState( ViewController.State.Paused );
        // Show the new modal view and update its state.
        this.modalViewController = view;
        modalViewController.onAttach( this );
        showView( modalViewController, ViewTransition.ShowModal );
        modalViewController.changeState( ViewController.State.Running );
    }

    public void dismissModalView() {
        if( modalViewController != null ) {
            modalViewController.changeState( ViewController.State.Stopped );
            showView( mainViewController, ViewTransition.HideModal );
            mainViewController.changeState( ViewController.State.Running );
            this.modalViewController = null;
        }
    }

    protected void showView(ViewController view, ViewTransition viewTransition) {
        if( viewContainer == null ) {
            return; // Can't show views if no view container.
        }
        // If the view doesn't specify a background colour then set to the app's default.
        if( view.getBackgroundColor() == Color.TRANSPARENT ) {
            view.setBackgroundColor( appBackgroundColor );
        }
        // Animate transition to the view controller, if the Android version supports it.
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            Transition transition;
            switch( viewTransition ) {
            case Replace:
                transition = new Fade();
                break;
            case ShowModal:
                transition = new Slide( Gravity.TOP );
                break;
            case HideModal:
                //transition = new Slide( Gravity.BOTTOM );
                transition = new Fade();
                break;
            default:
                transition = new Fade();
            }
            TransitionManager.beginDelayedTransition( viewContainer, transition );
        }
        // Create layout params for the view being shown.
        FrameLayout.LayoutParams layoutParams
            = new FrameLayout.LayoutParams( FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT );
        view.setLayoutParams( layoutParams );
        // Add / remove views as appropriate.
        switch( viewTransition ) {
        case Replace:
            if( mainViewController != null ) {
                viewContainer.removeView( mainViewController );
            }
            viewContainer.addView( view );
            break;
        case ShowModal:
            viewContainer.addView( view );
            break;
        case HideModal:
            viewContainer.removeView( modalViewController );
        }
        activeViewController = view;
        // Reset the title bar after changing the main view - the new view will apply its state
        // once it is resumed.
        view.setTitleBar( titleBar );
    }

    public TitleBar getTitleBar() {
        return titleBar;
    }

}