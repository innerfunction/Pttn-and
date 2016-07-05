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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.innerfunction.pttn.R;

/**
 * Created by juliangoacher on 26/04/16.
 */
public abstract class PttnActivity<T> extends AppCompatActivity {

    static final String Tag = PttnActivity.class.getSimpleName();

    public enum IntentActions { ViewUUID };

    /**
     * A UUID for the activity's view.
     * Used to fetch the view controller instance from the app container.
     */
    private String viewUUID;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent( intent );
        /*
        String dispatchAction = null;
        // Check for the app being opened using a custom URL.
        if( Intent.ACTION_VIEW.equals( intent.getAction() ) ) {
            Uri url = intent.getData();
            if( url != null ) {
                dispatchAction = url.toString();
            }
        }
        */
        viewUUID = intent.getStringExtra( IntentActions.ViewUUID.name() );
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.view_activity_layout );

        // TODO Assuming here that if the activity is being recreated then viewUUID will have been
        // TODO recovered by an onRestoreInstanceState(...) method call

        // Check for a container instantiated view.
        if( viewUUID != null ) {
            try {
                showView( (T)AppContainer.getAppContainer().getViewForUUID( viewUUID ) );
            }
            catch(ClassCastException e) {
                // Shouldn't happen if everything is setup correctly.
                Log.e( Tag, String.format( "View instance for %s is not the correct type", viewUUID ) );
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AppContainer.getAppContainer().setCurrentActivity( this );
    }

    @Override
    public void onPause() {
        super.onPause();
        AppContainer.getAppContainer().clearCurrentActivity( this );
    }

    @Override
    public void onDestroy() {
        if( viewUUID != null ) {
            AppContainer.getAppContainer().removeViewForUUID( viewUUID );
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if( viewUUID != null ) {
            bundle.putString( IntentActions.ViewUUID.name(), viewUUID );
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        viewUUID = bundle.getString( IntentActions.ViewUUID.name() );
    }

    public abstract void showView(T view);

}
