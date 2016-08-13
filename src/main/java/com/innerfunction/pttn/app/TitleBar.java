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
 * An interface providing a view controller with access to the screen title bar.
 * This is typically a wrapper for the parent activity.
 *
 * Created by juliangoacher on 25/07/16.
 */
public interface TitleBar {

    /** Hide (or show) the title bar. */
    void hideTitleBar(boolean hide);

    /** Set the screen title. */
    void setTitle(String title);

    /** Set the title bar text color. */
    void setTitleBarTextColor(int color);

    /** Set the title bar background color. */
    void setTitleBarColor(int color);

    /** Set the left-hand title bar button. */
    void setLeftTitleBarButton(TitleBarButton button);

}
