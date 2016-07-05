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
package com.innerfunction.pttn.ui.table;

import android.content.Context;
import android.util.AttributeSet;

import com.nakardo.atableview.foundation.NSIndexPath;
import com.nakardo.atableview.protocol.ATableViewDataSource;

/**
 * Additions to the base ATableView class.
 * Created by juliangoacher on 05/05/16.
 */
public class ATableView extends com.nakardo.atableview.view.ATableView {

    public ATableView(ATableViewStyle style, Context context) {
        super( style, context );
    }

    public ATableView(Context context) {
        super( context );
    }

    public ATableView(Context context, AttributeSet attrs) {
        super( context, attrs );
    }

    public ATableView(Context context, AttributeSet attrs, int defStyle) {
        super( context, attrs, defStyle );
    }

    public void scrollToRowWithIndexPath(final NSIndexPath indexPath) {
        this.post(new Runnable() {
            @Override
            public void run() {
                int row = 1;
                ATableViewDataSource dataSource = getDataSource();
                for( int s = 0; s < indexPath.getSection(); s++ ) {
                    row += dataSource.numberOfRowsInSection( ATableView.this, s ) + 1;
                }
                row += indexPath.getRow();
                ATableView.this.setSelection( row );
            }
        });
    }

}