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

import android.os.Build;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.innerfunction.pttn.R;

/**
 * The default app container activity.
 * Used to display view controller instances.
 *
 * Attached by juliangoacher on 19/05/16.
 */
public class ViewControllerActivity extends PttnActivity<ViewController> implements Chrome {

    /**
     * The currently displayed view controller.
     */
    private ViewController viewController;

    /**
     * The currently displayed modal view controller.
     */
    private ViewController modalViewController;

    enum ViewTransition { Replace, ShowModal, HideModal };

    @Override
    public void onStart() {
        super.onStart();
        if( viewController != null ) {
            viewController.changeState( ViewController.State.Started );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if( viewController != null ) {
            viewController.changeState( ViewController.State.Running );
        }
    }

    @Override
    public void onPause() {
        if( viewController != null ) {
            viewController.changeState( ViewController.State.Paused );
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if( viewController != null ) {
            viewController.changeState( ViewController.State.Stopped );
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if( viewController != null ) {
            viewController.changeState( ViewController.State.Destroyed );
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if( viewController == null || viewController.onBackPressed() ) {
            super.onBackPressed();
        }
    }

    // TODO View handling & state transitions in the code below need more thought. Code in
    // TODO ViewController.onDetachedFromWindow automatically transitions to Destroy, so e.g.
    // TODO the main view controller in the code below should go to Destroy when the modal is
    // TODO displayed; should instead (i) only hide the main view controller when showing the
    // TODO modal; and (ii) have some mechanism to reset destroyed views if/when reattached?
    
    @Override
    public void showView(ViewController view) {
        if( viewController == view ) {
            return;
        }
        if( viewController != null ) {
            viewController.changeState( ViewController.State.Stopped );
        }
        this.viewController = view;
        showView( viewController, ViewTransition.Replace );
    }

    public void showModalView(ViewController view) {
        if( modalViewController != null ) {
            dismissModalView();
        }
        this.viewController.changeState( ViewController.State.Paused );
        this.modalViewController = view;
        showView( modalViewController, ViewTransition.ShowModal );
    }

    public void dismissModalView() {
        if( modalViewController != null ) {
            modalViewController.changeState( ViewController.State.Stopped );
            showView( viewController, ViewTransition.HideModal );
            this.modalViewController = null;
        }
    }

    protected void showView(ViewController view, ViewTransition viewTransition) {
        view.onAttach( this );
        // Add the view controller.
        View mainView = findViewById( R.id.main );
        if( mainView instanceof ViewGroup ) {
            ViewGroup parentView = (ViewGroup)mainView.getParent();
            if( parentView != null ) {
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
                        transition = new Slide( Gravity.BOTTOM );
                        break;
                    default:
                        transition = new Fade();
                    }
                    TransitionManager.beginDelayedTransition( parentView, transition );
                }
                // ViewTransition to the view controller.
                int idx = parentView.indexOfChild( mainView );
                view.setId( mainView.getId() );
                // Copy layout params from the main view to the view controller.
                view.setLayoutParams( mainView.getLayoutParams() );
                // Remove the current view and replace with the new view.
                parentView.removeView( mainView );
                parentView.addView( view, idx );
            }
        }
        else {
            Log.w(Tag, "Main view placeholder in activity layout must be instance of ViewGroup");
        }
    }

    @Override
    public void hideTitleBar(boolean hide) {
        // TODO
    }

    @Override
    public void setTitle(String title) {
        // TODO
    }
}