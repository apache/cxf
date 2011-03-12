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

package org.apache.cxf.management.web.browser.client.service;

import javax.annotation.Nonnull;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

//TODO Remove - this class is useless
public abstract class AbstractCallback<T> implements RequestCallback {
    private static final int OK = 200;

    public void onResponseReceived(@Nonnull final Request request, @Nonnull final Response response) {
        if (OK == response.getStatusCode()) {
            onSuccess(parse(response));
        } else {
            
            // TODO add custom exception
            onError(request, new RuntimeException("Undefined remote service error"));
        }
    }

    public void onError(final Request request, final Throwable ex) {

        // TODO add custom exception
        throw new RuntimeException(ex);
    }

    public abstract void onSuccess(T obj);

    protected abstract T parse(Response response);    
}
