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

        LEFT( Gravity.START ), CENTER( Gravity.CENTER ), RIGHT( Gravity.END );

        int gravity;

        TextAlignment(int gravity) {
            this.gravity = gravity;
        }
    }

    private String fontName = "sans";
    private float fontSize = 12.0f;
    private int textColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private String textAlign = "left";
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
        this.textAlign = align;
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
        TextAlignment alignment = TextAlignment.valueOf( textAlign.toUpperCase() );
        if( alignment != null ) {
            textView.setGravity( alignment.gravity );
        }
    }
}
