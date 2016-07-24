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
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
     * The currently displayed view controller.
     */
    private ViewController viewController;

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

    @Override
    public void showView(ViewController viewController) {
        viewController.onAttach( this );
        // Add the view controller.
        View mainView = findViewById( R.id.main );
        if( mainView instanceof ViewGroup ) {
            ViewGroup parentView = (ViewGroup)mainView.getParent();
            if( parentView != null ) {
                // Animate transition to the view controller, if the Android version supports it.
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                    TransitionManager.beginDelayedTransition( parentView, new Fade() );
                }
                // Transition to the view controller.
                int idx = parentView.indexOfChild( mainView );
                viewController.setId( mainView.getId() );
                // Copy layout params from the main view to the view controller.
                viewController.setLayoutParams( mainView.getLayoutParams() );
                // Remove the current view and replace with the new view.
                parentView.removeView( mainView );
                parentView.addView( viewController, idx );
            }
        }
        else {
            Log.w(Tag, "Main view placeholder in activity layout must be instance of ViewGroup");
        }
        // Record the current view.
        this.viewController = viewController;
    }

}