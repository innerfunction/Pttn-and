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

    /**
     * Apply state to the title bar.
     * The provided state is applied on top of any existing state. This allows the title bar's
     * final state to be built up by compounding the configured states of a number of views.
     * @param state The state to apply to this title bar.
     */
    void applyState(TitleBarState state);

}
