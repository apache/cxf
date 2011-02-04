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

package org.apache.cxf.management.web.browser.client.service.browser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;

import static com.google.gwt.http.client.RequestBuilder.GET;
import static com.google.gwt.http.client.RequestBuilder.Method;

import org.apache.cxf.management.web.browser.client.service.AbstractCallback;

public class FeedProxyImpl implements FeedProxy {
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String ATOM_TYPE = "application/atom+xml";

    public void getFeed(@Nonnull final String url, @Nonnull final RequestCallback callback) {
        RequestBuilder builder = buildRequest(GET, url);

        try {
            builder.sendRequest(null, callback);
        } catch (RequestException ex) {

            // TODO add custom exception
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    private RequestBuilder buildRequest(@Nonnull final Method method, @Nonnull final String url) {
        assert !"".equals(url);
        
        RequestBuilder builder = new RequestBuilder(method, url);
        builder.setHeader(CONTENT_TYPE_HEADER, ATOM_TYPE);
        builder.setHeader(ACCEPT_HEADER, ATOM_TYPE);

        return builder;
    }

    public abstract static class Callback extends AbstractCallback<Feed> {
        
        @Override @Nonnull
        protected Feed parse(@Nonnull final Response response) {
            Document document = convertFromXML(response);
            if (document != null) {
                return new Feed(document);
            } else {
                return Feed.EMPTY;
            }
        }

        @Nullable
        private Document convertFromXML(@Nonnull final Response response) {
            if (response.getText() != null) {
                return XMLParser.parse(response.getText());
            } else {
                return null;
            }
        }
    }
}
