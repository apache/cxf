/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.management.web.browser.client.service.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class LocalStorageImpl implements LocalStorage {
    private static final String SETTINGS_KEY = "logBrowser.settings";

    private Boolean isAvailable;

    public boolean isAvailable() {
        if (isAvailable == null) {
            this.isAvailable = checkIsAvailable();
        }
        return this.isAvailable;
    }

    private native boolean checkIsAvailable() /*-{
        var key = "isLocalStorageAvailable";
        $wnd.$.jStorage.set(key, true);
        return !($wnd.$.jStorage.get(key) == null);
    }-*/;

    public void saveSettings(@Nonnull final Settings settings) {
        if (isAvailable()) {
            put(SETTINGS_KEY, Converter.convertToLocalSettings(settings));
        }
    }

    @Nullable
    public Settings retrieveSettings() {
        if (isAvailable()) {
            LocalSettings localSettings = (LocalSettings) get(SETTINGS_KEY);
            if (localSettings != null) {
                return Converter.convertToSettings(localSettings);
            }
        }
        return null;
    }

    public void clear() {
        if (isAvailable()) {
            remove(SETTINGS_KEY);
        }
    }

    private native void put(@Nonnull final String key, @Nonnull final JavaScriptObject obj) /*-{
        $wnd.$.jStorage.set(key, obj);
    }-*/;

    @Nullable
    private native JavaScriptObject get(@Nonnull final String key) /*-{
        return $wnd.$.jStorage.get(key);
    }-*/;

    private native void remove(@Nonnull final String key) /*-{
        $wnd.$.jStorage.deleteKey(key);
    }-*/;

    private static final class Converter {

        private Converter() { }

        @Nullable
        public static Settings convertToSettings(@Nullable final LocalSettings src) {
            if (src == null) {
                return null;
            }

            Settings dst = new Settings();
            dst.setCredentials(convertToCredentials(src.getCredentials()));
            JsArray<LocalSubscription> subscriptions = src.getSubscriptions();
            for (int i = 0; i < subscriptions.length(); i++) {
                Subscription subscription = convertToSubscription(subscriptions.get(i));
                if (subscription != null) {
                    dst.getSubscriptions().add(subscription);
                }
            }

            return dst;
        }

        @Nullable
        private static Credentials convertToCredentials(@Nullable final LocalCredentials src) {
            if (src == null) {
                return null;
            }

            return new Credentials(src.getUsername(), src.getPassword());
        }

        @Nullable
        private static Subscription convertToSubscription(@Nullable final LocalSubscription src) {
            if (src == null) {
                return null;
            }

            return new Subscription(src.getId(), src.getName(), src.getURL());
        }

        @SuppressWarnings("unchecked") @Nullable
        public static LocalSettings convertToLocalSettings(@Nullable final Settings src) {
            if (src == null) {
                return null;
            }

            LocalSettings dst = (LocalSettings) JavaScriptObject.createObject();

            dst.setCredentials(convertToLocalCredentials(src.getCredentials()));

            JsArray<LocalSubscription> dstSubscriptions =
                (JsArray<LocalSubscription>) JavaScriptObject.createArray();

            for (Subscription subscription : src.getSubscriptions()) {
                dstSubscriptions.push(convertToLocalSubscription(subscription));
            }

            dst.setSubscriptions(dstSubscriptions);

            return dst;

        }

        @Nullable
        private static LocalCredentials convertToLocalCredentials(@Nullable final Credentials src) {
            if (src == null) {
                return null;
            }

            LocalCredentials dst = (LocalCredentials) JavaScriptObject.createObject();

            dst.setUsername(src.getUsername());
            dst.setPassword(src.getPassword());

            return dst;
        }

        @Nullable
        private static LocalSubscription convertToLocalSubscription(@Nullable final Subscription src) {
            if (src == null) {
                return null;
            }

            LocalSubscription dst = (LocalSubscription) JavaScriptObject.createObject();

            dst.setId(src.getId());
            dst.setName(src.getName());
            dst.setURL(src.getUrl());

            return dst;
        }
    }

    public static class LocalSettings extends JavaScriptObject {

        protected LocalSettings() { }
    
        public final native void setCredentials(@Nullable final LocalCredentials credentials) /*-{
            this.credentials = credentials;
        }-*/;

        @Nullable
        public final native LocalCredentials getCredentials() /*-{
            return this.credentials;
        }-*/;

        public final native void setSubscriptions(@Nullable JsArray<LocalSubscription> subscriptions) /*-{
            this.subscriptions = subscriptions;
        }-*/;

        @Nonnull
        public final native JsArray<LocalSubscription> getSubscriptions() /*-{
            if (this.subscriptions != null) {
                return this.subscriptions;
            } else {
                return [];
            }
        }-*/;
    }
    @SuppressWarnings("unused")
    private static class LocalCredentials extends JavaScriptObject {

        protected LocalCredentials() { }

        public final native void setUsername(@Nullable final String username) /*-{
            this.username = username;
        }-*/;

        @Nullable
        public final native String getUsername() /*-{
            return this.username;
        }-*/;

        public final native void setPassword(@Nullable final String password) /*-{
            this.password = password;
        }-*/;

        @Nullable
        public final native String getPassword() /*-{
             return this.password;
        }-*/;
    }

    @SuppressWarnings("unused")
    private static class LocalSubscription extends JavaScriptObject {

        protected LocalSubscription() { }

        public final native void setId(@Nullable final String id) /*-{
            this.id = id;
        }-*/;

        @Nullable
        public final native String getId() /*-{
            return this.id;
        }-*/;

        public final native void setName(@Nullable final String name) /*-{
            this.name = name;
        }-*/;

        @Nullable
        public final native String getName() /*-{
            return this.name;
        }-*/;

        public final native void setURL(@Nullable final String url) /*-{
            this.url = url;
        }-*/;

        @Nullable
        public final native String getURL() /*-{
            return this.url;
        }-*/;
    }

}
