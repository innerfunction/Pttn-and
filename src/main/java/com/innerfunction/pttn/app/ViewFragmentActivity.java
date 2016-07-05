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

import android.app.FragmentTransaction;

import com.innerfunction.pttn.R;

/**
 * Used to display view fragments created by the app container.
 *
 * Created by juliangoacher on 26/04/16.
 */
public class ViewFragmentActivity extends PttnActivity<ViewFragment> {

    /**
     * The currently displayed fragment.
     * Will be null if the current fragment isn't an instance of ViewFragment.
     */
    private ViewFragment viewFragment;

    @Override
    public void onBackPressed() {
        if( viewFragment == null || viewFragment.onBackPressed() ) {
            super.onBackPressed();
        }
    }

    @Override
    public void showView(ViewFragment view) {
        // Add the view fragment.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace( R.id.main, view );
        ft.commit();
        // Record the current fragment.
        this.viewFragment = view;
    }

}
