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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.res.Resources;

/**
 * A Map that returns localized strings for resource ID keys.
 * Intended for use with StringTemplate, to provide localized values within templates.
 * @author juliangoacher
 */
public class I18nMap implements Map<String, String> {

    private Resources r;
    private String packageName;

    public I18nMap(Context context) {
        this.r = context.getResources();
        this.packageName = context.getPackageName();
    }

    public String getLocalizedString(String resourceID) {
        int rid = this.r.getIdentifier( resourceID, "string", this.packageName );
        return rid > 0 ? this.r.getString( rid ) : null;
    }

    @Override
    public void clear() {
        // Noop.
    }

    @Override
    public boolean containsKey(Object key) {
        return getLocalizedString( (String)key ) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Set<java.util.Map.Entry<String, String>> entrySet() {
        return null;
    }

    @Override
    public String get(Object key) {
        String s = getLocalizedString( (String)key );
        return s == null ? key.toString() : s;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public String put(String key, String value) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> arg0) {
        // Noop
    }

    @Override
    public String remove(Object key) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Collection<String> values() {
        return null;
    }

}
