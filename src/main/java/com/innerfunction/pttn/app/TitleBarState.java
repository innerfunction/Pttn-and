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
 * A class for representing the configured state of a view's title bar.
 * Created by juliangoacher on 19/08/16.
 */
public class TitleBarState implements TitleBar {

    /** The title bar being controlled by this state. */
    private TitleBar titleBar = new TitleBarStub();
    private boolean hidden = false;
    private String title = "";
    private int color;
    private int textColor;
    private TitleBarButton leftButton;
    private TitleBarButton rightButton;

    public void setTitleBar(TitleBar titleBar) {
        this.titleBar = titleBar;
    }

    public TitleBar getTitleBar() {
        return titleBar;
    }

    public void setTitleBarHidden(boolean hide) {
        this.hidden = hide;
    }

    /** Get the title bar's hidden state. */
    boolean getTitleBarHidden() {
        return hidden;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitleBarTextColor(int color) {
        this.color = color;
    }

    public int getTitleBarTextColor() {
        return color;
    }

    public void setTitleBarColor(int color) {
        this.textColor = color;
    }

    public int getTitleBarColor() {
        return color;
    }

    public void setLeftTitleBarButton(TitleBarButton button) {
        this.leftButton = button;
    }

    public TitleBarButton getLeftTitleBarButton() {
        return leftButton;
    }

    public void setRightTitleBarButton(TitleBarButton button) {
        this.rightButton = button;
    }

    public TitleBarButton getRightTitleBarButton() {
        return rightButton;
    }

    /** Apply the state to the action bar. */
    public void apply() {
        titleBar.applyState( this );
    }

    @Override
    public void applyState(TitleBarState state) {
        // Apply the combination of the current state and the argument state to the title bar.
        titleBar.applyState( new Union( state ) );
    }

    /**
     * A combination of this title bar state with another state.
     * Note that for most title bar properties, the other state's property setting takes
     * precedence over this state's.
     */
    class Union extends TitleBarState {

        private TitleBarState state;

        Union(TitleBarState state) {
            this.state = state;
        }

        public boolean getTitleBarHidden() {
            return hidden || state.getTitleBarHidden();
        }

        public String getTitle() {
            return notNull( state.getTitle(), title );
        }

        public int getTitleBarColor() {
            return notZero( state.getTitleBarColor(), color );
        }

        public int getTitleBarTextColor() {
            return notZero( state.getTitleBarTextColor(), textColor );
        }

        public TitleBarButton getLeftTitleBarButton() {
            return notNull( state.getLeftTitleBarButton(), leftButton );
        }

        public TitleBarButton getRightTitleBarButton() {
            return notNull( state.getRightTitleBarButton(), rightButton );
        }

        <T> T notNull(T a, T b) {
            return a != null ? a : b;
        }

        int notZero(int a, int b) {
            return a != 0 ? a : b;
        }
    }
}
