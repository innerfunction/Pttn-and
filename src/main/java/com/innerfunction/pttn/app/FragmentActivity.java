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

import com.innerfunction.pttn.R;

/**
 * An activity class for displaying standard fragments which have been instantiated by the app
 * container.
 *
 * Attached by juliangoacher on 19/05/16.
 */
public class FragmentActivity extends PttnActivity<Fragment> {

    @Override
    public void showView(Fragment view) {
        // Add the view fragment.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace( R.id.main, view );
        ft.commit();
    }

}
