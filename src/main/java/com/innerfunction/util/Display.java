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
package com.innerfunction.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Utility methods providing useful display related functionality.
 * Created by juliangoacher on 28/07/16.
 */
public class Display {

    public static DisplayMetrics getDisplayMetrics() {
        return Resources.getSystem().getDisplayMetrics();
    }

    /**
     * Convert a device-independent pixel value to actual screen pixels.
     * @param dp    A DP value.
     * @return      A pixel value.
     */
    public static int dpToPx(int dp) {
        return dpToPx( (float)dp );
    }

    /**
     * Convert a device-independent pixel value to actual screen pixels.
     * @param dp    A DP value.
     * @return      A pixel value.
     */
    public static int dpToPx(float dp) {
        //float px = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, dp, getDisplayMetrics() );
        float px = dp * (getDisplayMetrics().densityDpi / 160f);
        return Math.round( px );
    }

    /**
     * Convert a point size value to scaled pixels.
     * @param   pt  A point value.
     * @return  A scaled pixel value.
     */
    public static int ptToSp(int pt) {
        return ptToSp( (float)pt );
    }

    /**
     * Convert a point size value to scaled pixels.
     * Point values are calculated as 0.85 of the DIP value.
     * @param   pt  A point value.
     * @return  A scaled pixel value.
     */
    public static int ptToSp(float pt) {
        float px = dpToPx( pt * 1.75f );
        //float sp = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_SP, pt, getDisplayMetrics() );
        float sp = px / getDisplayMetrics().scaledDensity; // see http://stackoverflow.com/a/9219417
        return Math.round( sp );
    }

    /**
     * Convert an actual-pixel value to a device-independent pixel value.
     * @param px    A pixel value.
     * @return      A DP value.
     */
    public static int pxToDp(int px) {
        return pxToDp( (float)px );
    }

    /**
     * Convert an actual-pixel value to a device-independent pixel value.
     * @param px    A pixel value.
     * @return      A DP value.
     */
    public static int pxToDp(float px) {
        //float dp = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_PX, px, getDisplayMetrics() );
        float dp = px / (getDisplayMetrics().densityDpi / 160f);
        return Math.round( dp );
    }

}
