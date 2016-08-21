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

import android.graphics.drawable.Drawable;

/**
 * A class used to represent a title bar button's settings.
 * Created by juliangoacher on 13/08/16.
 */
public class TitleBarButton {

    private ViewController owner;
    private String title;
    private Drawable image;
    private String action;

    public TitleBarButton() {}

    public TitleBarButton(String title, Drawable image, String action) {
        this.title = title;
        this.image = image;
        this.action = action;
    }

    public void setOwner(ViewController owner) {
        this.owner = owner;
    }

    public ViewController getOwner() {
        return owner;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public Drawable getImage() {
        return image;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
