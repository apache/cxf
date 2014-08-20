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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.apache.cxf.management.web.browser.client.service.AbstractCallback;

public class RemoteStorageProxyImpl implements RemoteStorageProxy {
    private static final String HOSTED_MODE_ENDPOINT_URL = "settings";
    private static final String RESOURCES_ENDPOINT_SUFFIX = "/resources/";
    private static final String SETTINGS_ENDPOINT_SUFFIX = "/settings";
    
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String ACCEPT_HEADER = "Accept";    
    private static final String JSON_TYPE = "application/json";

    private static final String SETTINGS_KEY = "settings";

    private String endpointURL;

    public void saveSettings(@Nonnull final RequestCallback callback) {
        RequestBuilder builder = buildRequest(RequestBuilder.GET, buildEndpointURL());
        execute(null, builder, callback);
    }

    public void retrieveSettings(@Nonnull final Settings settings, @Nonnull final RequestCallback callback) {
        RequestBuilder builder = buildRequest(RequestBuilder.PUT, buildEndpointURL());
        execute(Converter.convertToRemoteSettings(settings), builder, callback);
    }

    @Nonnull
    private String buildEndpointURL() {
        if (endpointURL == null) {
            String baseURL = GWT.getHostPageBaseURL();
            if (baseURL.endsWith(RESOURCES_ENDPOINT_SUFFIX)) {

                // compute URL by replace "resources" suffix with "settings" suffix
                endpointURL = cutResourcesSuffix(baseURL) + SETTINGS_ENDPOINT_SUFFIX;
            } else {

                // it ought to execute only in hosted mode, because there is diffrent base URL
                endpointURL = HOSTED_MODE_ENDPOINT_URL;
            }
        }

        return endpointURL;
    }

    @Nonnull
    private String cutResourcesSuffix(@Nonnull final String url) {
        return url.substring(0, url.lastIndexOf(RESOURCES_ENDPOINT_SUFFIX));
    }

    @Nonnull
    private RequestBuilder buildRequest(@Nonnull final RequestBuilder.Method method,
                                        @Nonnull final String url) {
        assert !"".equals(url);
        
        RequestBuilder builder = new RequestBuilder(method, url);
        builder.setHeader(CONTENT_TYPE_HEADER, JSON_TYPE);
        builder.setHeader(ACCEPT_HEADER, JSON_TYPE);

        return builder;
    }

    private void execute(@Nullable final RemoteSettings remoteSettings,
                         @Nonnull final RequestBuilder builder,
                         @Nonnull final RequestCallback callback) {
        String json = null;
        if (remoteSettings != null) {

            // TODO add appropriate comment - wrap  
            JSONObject rootElement = new JSONObject();
            rootElement.put(SETTINGS_KEY, new JSONObject(remoteSettings));
            json = rootElement.toString();
        }

        try {
            builder.sendRequest(json, callback);
        } catch (RequestException ex) {
            
            // TODO add custom exception
            throw new RuntimeException(ex);
        }
    }

    protected static class RemoteSettings extends JavaScriptObject {

        protected RemoteSettings() {
        }

        public final native void setSubscriptions(@Nullable JsArray<RemoteSubscription> subscriptions) /*-{
            this.subscriptions = subscriptions;
        }-*/;

        @Nonnull
        public final native JsArray<RemoteSubscription> getSubscriptions() /*-{
            if (this.subscriptions != null) {
                return this.subscriptions;
            } else {
                return [];
            }
        }-*/;
    }

    protected static class RemoteSubscription extends JavaScriptObject {

        protected RemoteSubscription() {
        }

        public final native void setId(@Nullable final String id) /*-{
            this.id = id;
        }-*/;

        @Nullable
        public final native String getId() /*-{
            return this.id;
        }-*/;

        public final native void setUrl(@Nullable final String url) /*-{
            this.url = url;
        }-*/;

        @Nullable
        public final native String getUrl() /*-{
            return this.url;
        }-*/;        

        public final native void setName(@Nullable final String name) /*-{
            this.name = name;
        }-*/;

        @Nullable
        public final native String getName() /*-{
            return this.name;
        }-*/;
    }

    protected static final class Converter {

        private Converter() {
        }

        @Nonnull
        public static Settings convertToSettings(@Nonnull final RemoteSettings remoteSettings) {
            Settings settings = new Settings();

            JsArray<RemoteSubscription> remoteSubscriptions = remoteSettings.getSubscriptions();
            for (int i = 0; i < remoteSubscriptions.length(); i++) {
                settings.getSubscriptions().add(convertToSubscription(remoteSubscriptions.get(i)));
            }

            return settings;
        }

        @Nonnull
        public static Subscription convertToSubscription(@Nonnull RemoteSubscription remoteSubscription) {
            return new Subscription(remoteSubscription.getId(),
                remoteSubscription.getName(), remoteSubscription.getUrl());
        }

        @SuppressWarnings("unchecked") @Nonnull
        public static RemoteSettings convertToRemoteSettings(@Nonnull final Settings settings) {
            RemoteSettings remoteSettings = (RemoteSettings) JavaScriptObject.createObject();

            JsArray<RemoteSubscription> remoteSubscriptions =
                (JsArray<RemoteSubscription>) JavaScriptObject.createArray();

            for (Subscription subscription : settings.getSubscriptions()) {
                remoteSubscriptions.push(convertToRemoteSubscription(subscription));
            }

            remoteSettings.setSubscriptions(remoteSubscriptions);

            return remoteSettings;
        }

        @Nonnull
        public static RemoteSubscription convertToRemoteSubscription(@Nonnull Subscription subscription) {
            RemoteSubscription remoteSubscription = (RemoteSubscription) JavaScriptObject.createObject();

            remoteSubscription.setId(subscription.getId());
            remoteSubscription.setName(subscription.getName());
            remoteSubscription.setUrl(subscription.getUrl());

            return remoteSubscription;
        }
    }

    public abstract static class Callback extends AbstractCallback<Settings> {

        @Override @Nullable
        protected Settings parse(@Nonnull final Response response) {
            RemoteSettings result = null;

            if (response.getText() != null && !"".equals(response.getText())) {

                // TODO add appropriate comment - unwrap
                JSONValue rootElement = new JSONObject(convertFromJSON(response.getText())).get(SETTINGS_KEY);
                if (rootElement != null && rootElement.isObject() != null) {
                    result = (RemoteSettings) rootElement.isObject().getJavaScriptObject();
                }
            }

            return result != null ? Converter.convertToSettings(result) : null;
        }

        // TODO check if this method must be final regardless to GWT's guidelines
        // CHECKSTYLE:OFF
        @Nonnull
        private final native JavaScriptObject convertFromJSON(@Nonnull final String json) /*-{
            return $wnd.JSON.parse(json);
        }-*/;
        // CHECKSTYLE:ON
    }

    public static class NoActionCallback extends AbstractCallback<Settings> {

        @Override
        public void onSuccess(@Nullable final Settings obj) { }

        @Override @Nullable
        protected Settings parse(@Nonnull final Response response) {
            return null;
        }
    }
}
