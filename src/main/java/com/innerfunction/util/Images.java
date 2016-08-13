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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.innerfunction.http.Client;
import com.innerfunction.http.Response;
import com.innerfunction.q.Q;

import java.io.File;
import java.net.MalformedURLException;

public class Images {

    /**
     * Load an image from a URL.
     * @param url       Either a file: or HTTP URL.
     * @param context   An Android context object.
     * @return          A deferred promise resolving to the image as a Drawable instance.
     *                  The promise will resolve to null if a valid image can't be loaded from the
     *                  URL.
     */
    public static Q.Promise<Drawable> loadImageFromURL(String url, Context context) {
        int idx = url.indexOf(':');
        if( idx == -1 ) {
            return Q.reject("Invalid URL");
        }
        String scheme = url.substring( 0, idx );
        if( "file".equals( scheme ) ) {
            String path = url.substring( 7 );
            Bitmap bitmap = BitmapFactory.decodeFile( path );
            Drawable drawable = new BitmapDrawable( Resources.getSystem(), bitmap );
            return Q.resolve( drawable );
        }
        else if( "http".equals( scheme ) || "https".equals( scheme ) ) {
            Client httpClient = new Client( context );
            try {
                return httpClient.get( url )
                    .then( new Q.Promise.Callback<Response, Drawable>() {
                        @Override
                        public Drawable result(Response response) {
                            Drawable drawable = null;
                            String contentType = response.getContentType();
                            if( contentType != null && contentType.startsWith("image/") ) {
                                byte[] data = response.getRawBody();
                                Bitmap bitmap = BitmapFactory.decodeByteArray( data, 0, data.length );
                                drawable = new BitmapDrawable( Resources.getSystem(), bitmap );
                            }
                            return drawable;
                        }
                    } );
            }
            catch(MalformedURLException e) {
                Q.reject( e );
            }
        }
        return Q.reject( String.format("Unsupported URL scheme: %s", scheme ) );
    }

    /**
     * Convert an image resource name to a resource ID.
     */
    public static int imageNameToResourceID(String name, Context context) {
        return imageNameToResourceID( name, context.getResources(), context );
    }

    /**
     * Convert an image resource name to a resource ID.
     */
    public static int imageNameToResourceID(String name, Resources r, Context context) {
        // Convert forward slashes to double underscore;
        String rid = name.replaceAll("/", "__");
        // Remove any trailing file extension;
        int idx = rid.indexOf('.');
        if( idx > 0 ) {
            rid = rid.substring( 0, idx );
        }
        return r.getIdentifier( rid, "drawable", context.getPackageName() );
    }

    /**
     * Zooming image
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidht = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidht, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return newbmp;
    }

    /**
     * Drawable to Bitmap
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height,
            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;

    }

    /**
     * 把图片变成圆角
     * @param bitmap 需要修改的图片
     * @param pixels 圆角的弧度
     * @return 圆角图片
     */
    public static Bitmap toRoundedCorner(Bitmap bitmap, float radius) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
            .getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }


    /**
     * 使圆角功能支持BitampDrawable
     * @param bitmapDrawable
     * @param pixels
     * @return
     */
    @SuppressWarnings("deprecation")
    public static BitmapDrawable toRoundedCorner(BitmapDrawable bitmapDrawable, float radius) {
        Bitmap bitmap = bitmapDrawable.getBitmap();
        bitmapDrawable = new BitmapDrawable(toRoundedCorner(bitmap, radius));
        return bitmapDrawable;
    }

    /**
     * Get Bitmap Reflection
     */
    public static Bitmap createReflectionImageWithOrigin(Bitmap bitmap) {
        final int reflectionGap = 4;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

        Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, height / 2, width, height / 2,
            matrix, false);

        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height / 2),
            Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmapWithReflection);
        canvas.drawBitmap(bitmap, 0, 0, null);
        Paint defaultPaint = new Paint();
        canvas.drawRect(0, height, width, height + reflectionGap, defaultPaint);

        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0,
            bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff, 0x00ffffff,
            TileMode.CLAMP);
        paint.setShader(shader);
        // Set the Transfer mode to be porter duff and destination in
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        // Draw a rectangle using the paint with our linear gradient
        canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

        return bitmapWithReflection;
    }

}
