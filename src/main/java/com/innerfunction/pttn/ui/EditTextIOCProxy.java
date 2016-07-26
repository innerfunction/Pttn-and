package com.innerfunction.pttn.ui;

import android.text.InputType;
import android.widget.EditText;

import com.innerfunction.pttn.IOCProxy;

/**
 * Created by juliangoacher on 26/07/16.
 */
public class EditTextIOCProxy implements IOCProxy {

    enum KeyboardType {

        DEFAULT( InputType.TYPE_CLASS_TEXT ),
        WEB( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI ),
        NUMBER( InputType.TYPE_CLASS_NUMBER ),
        PHONE( InputType.TYPE_CLASS_PHONE ),
        EMAIL( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS );

        int mask;

        KeyboardType(int mask) {
            this.mask = mask;
        }
    }

    enum AutocapitalizationMode {

        NONE( 0 ),
        WORDS( 0 ),
        SENTENCES( 0 ),
        ALL( 0 );

        int mask;

        AutocapitalizationMode(int mask) {
            this.mask = mask;
        }
    }

    private EditText value;
    private String text;
    private TextStyle style = new TextStyle();
    private String keyboard = "default";
    private String autocapitalization = "none";
    private String autocorrection = "none";

    @Override
    public void initializeWithValue(Object value) {
        this.value = (EditText)value;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setStyle(TextStyle style) {
        this.style = style;
    }

    public TextStyle getStyle() {
        return style;
    }

    public void setKeyboard(String keyboard) {
        this.keyboard = keyboard;
    }

    public void setAutocapitalization(String autocapitalization) {
        this.autocapitalization = autocapitalization;
    }

    public void setAutocorrection(String autocorrection) {
        this.autocorrection = autocorrection;
    }

    @Override
    public Object unwrapValue() {
        if( value == null ) {
            // TODO: Initialize value.
        }
        return this;
    }
}
