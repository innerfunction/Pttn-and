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
package com.innerfunction.pttn.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.TextView;

/**
 * An object providing a standard configuration interface for Android text fields.
 * Created by juliangoacher on 26/07/16.
 */
public class TextStyle {

    enum TextAlignment {

        LEFT( Gravity.START ),
        CENTER( Gravity.CENTER ),
        RIGHT( Gravity.END );

        int gravity;

        TextAlignment(int gravity) {
            this.gravity = gravity;
        }

        static TextAlignment parse(String alignment) {
            try {
                return valueOf( alignment.toUpperCase() );
            }
            catch(Exception e) {
                return LEFT;
            }
        }
    }

    private String fontName = "sans";
    private float fontSize = 12.0f;
    private int textColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private TextAlignment textAlign = TextAlignment.LEFT;
    private boolean bold;
    private boolean italic;

    public void setFontName(String name) {
        this.fontName = name;
    }

    public void setFontSize(float size) {
        this.fontSize = size;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setTextAlign(String align) {
        this.textAlign = TextAlignment.parse( align );
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public void applyToTextView(TextView textView) {
        int style = 0;
        if( bold ) {
            style &= Typeface.BOLD;
        }
        if( italic ) {
            style &= Typeface.ITALIC;
        }
        // TODO: Do any font name conversions need to be done here?
        Typeface typeface = Typeface.create( fontName, style );
        if( typeface != null ) {
            textView.setTypeface( typeface );
        }
        textView.setTextSize( fontSize );
        textView.setTextColor( textColor );
        textView.setBackgroundColor( backgroundColor );
        textView.setGravity( textAlign.gravity );
    }
}
