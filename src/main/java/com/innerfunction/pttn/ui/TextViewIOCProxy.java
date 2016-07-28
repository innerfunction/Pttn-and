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

import android.content.Context;
import android.text.InputType;
import android.widget.TextView;

import com.innerfunction.pttn.IOCProxy;

/**
 * A configuration proxy for text views.
 * Created by juliangoacher on 26/07/16.
 */
public class TextViewIOCProxy implements IOCProxy {

    enum KeyboardType {

        DEFAULT( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL ),
        WEB( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI ),
        NUMBER( InputType.TYPE_CLASS_NUMBER ),
        PHONE( InputType.TYPE_CLASS_PHONE ),
        EMAIL( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS );

        int mask;

        KeyboardType(int mask) {
            this.mask = mask;
        }

        static KeyboardType parse(String value) {
            try {
                return valueOf( value.toUpperCase() );
            }
            catch(Exception e) {
                return DEFAULT;
            }
        }
    }

    enum AutocapitalizationMode {

        NONE( 0 ),
        WORDS( InputType.TYPE_TEXT_FLAG_CAP_WORDS ),
        SENTENCES( InputType.TYPE_TEXT_FLAG_CAP_SENTENCES ),
        ALL( InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS );

        int mask;

        AutocapitalizationMode(int mask) {
            this.mask = mask;
        }

        static AutocapitalizationMode parse(String value) {
            try {
                return valueOf( value.toUpperCase() );
            }
            catch(Exception e) {
                return NONE;
            }
        }

    }

    enum AutocorrectionMode {

        NONE( InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS ),
        OFF( InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS ),
        ON( InputType.TYPE_TEXT_FLAG_AUTO_CORRECT );

        int mask;

        AutocorrectionMode(int mask) {
            this.mask = mask;
        }

        static AutocorrectionMode parse(String value) {
            try {
                return valueOf( value.toUpperCase() );
            }
            catch(Exception e) {
                return NONE;
            }
        }
    }

    private TextView textView;
    private String text;
    private TextStyle style = new TextStyle();
    private KeyboardType keyboard = KeyboardType.DEFAULT;
    private AutocapitalizationMode autocapitalization = AutocapitalizationMode.NONE;
    private AutocorrectionMode autocorrection = AutocorrectionMode.NONE;

    public TextViewIOCProxy(Context context) {
        this.textView = new TextView( context );
    }

    public TextViewIOCProxy(TextView textView) {
        this.textView = textView;
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
        this.keyboard = KeyboardType.parse( keyboard );
    }

    public void setAutocapitalization(String autocapitalization) {
        this.autocapitalization = AutocapitalizationMode.parse( autocapitalization );
    }

    public void setAutocorrection(String autocorrection) {
        this.autocorrection = AutocorrectionMode.parse( autocorrection );
    }

    @Override
    public Object unwrapValue() {
        if( text != null ) {
            textView.setText( text );
        }
        style.applyToTextView( textView );
        textView.setInputType( keyboard.mask | autocapitalization.mask | autocorrection.mask );
        return textView;
    }
}
