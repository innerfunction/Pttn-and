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

/**
 * A class for modelling a title bar as a combination of two states.
 * Created by juliangoacher on 21/08/16.
 */
public class VirtualTitleBar extends TitleBarState implements TitleBar {

    private TitleBarState otherState = new TitleBarState();

    public boolean getTitleBarHidden() {
        return super.getTitleBarHidden() || otherState.getTitleBarHidden();
    }

    public String getTitle() {
        String title = otherState.getTitle();
        if( title == null || title.equals("") ) {
            title = super.getTitle();
        }
        return title;
    }

    public int getTitleBarColor() {
        return notZero( otherState.getTitleBarColor(), super.getTitleBarColor() );
    }

    public int getTitleBarTextColor() {
        return notZero( otherState.getTitleBarTextColor(), super.getTitleBarTextColor() );
    }

    public TitleBarButton getLeftTitleBarButton() {
        return notNull( otherState.getLeftTitleBarButton(), super.getLeftTitleBarButton() );
    }

    public TitleBarButton getRightTitleBarButton() {
        return notNull( otherState.getRightTitleBarButton(), super.getRightTitleBarButton() );
    }

    <T> T notNull(T a, T b) {
        return a != null ? a : b;
    }

    int notZero(int a, int b) {
        return a != 0 ? a : b;
    }

    @Override
    public void applyState(TitleBarState state) {
        this.otherState = state;
        getTitleBar().applyState( this );
    }

}
