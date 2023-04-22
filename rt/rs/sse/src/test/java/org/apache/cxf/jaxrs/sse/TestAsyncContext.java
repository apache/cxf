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

package org.apache.cxf.jaxrs.sse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class TestAsyncContext implements AsyncContext {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Collection<AsyncListener> listeners = new ArrayList<>();

    TestAsyncContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public ServletRequest getRequest() {
        return this.request;
    }

    @Override
    public ServletResponse getResponse() {
        return this.response;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        throw new UnsupportedOperationException("The operation is not supported by test implementation");
    }

    @Override
    public void dispatch() {
        throw new UnsupportedOperationException("The operation is not supported by test implementation");
    }

    @Override
    public void dispatch(String path) {
        throw new UnsupportedOperationException("The operation is not supported by test implementation");
    }

    @Override
    public void dispatch(ServletContext context, String path) {
        throw new UnsupportedOperationException("The operation is not supported by test implementation");
    }

    @Override
    public void complete() {
        for (AsyncListener listener : this.listeners) {
            try {
                listener.onComplete(new AsyncEvent(this, this.request, this.response));
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    @Override
    public void start(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void addListener(AsyncListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest req, ServletResponse resp) {
        this.listeners.add(listener);
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException("The operation is not supported by test implementation");
    }
    
    @Override
    public void setTimeout(long timeout) {
        throw new UnsupportedOperationException("The operation is not supported by test implementation");
    }

    @Override
    public long getTimeout() {
        throw new UnsupportedOperationException("The operation is not supported by test implementation");
    }
}
