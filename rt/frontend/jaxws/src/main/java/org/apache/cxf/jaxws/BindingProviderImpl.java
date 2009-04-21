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

package org.apache.cxf.jaxws;

import java.lang.ref.WeakReference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;

public class BindingProviderImpl implements BindingProvider {
    protected ThreadLocal <WeakReference<Map<String, Object>>> requestContext =
        new ThreadLocal<WeakReference<Map<String, Object>>>();
    protected ThreadLocal <WeakReference<Map<String, Object>>> responseContext =
        new ThreadLocal<WeakReference<Map<String, Object>>>();
    private final Binding binding;
    private final EndpointReferenceBuilder builder;

    public BindingProviderImpl() {
        this.binding = null;
        this.builder = null;
    }

    public BindingProviderImpl(Binding b) {
        this.binding = b;
        this.builder = null;
    }

    public BindingProviderImpl(JaxWsEndpointImpl endpoint) {
        this.binding = endpoint.getJaxwsBinding();
        this.builder = new EndpointReferenceBuilder(endpoint);
    }

    public Map<String, Object> getRequestContext() {
        return getThreadRequestContext();
    }

    public Map<String, Object> getResponseContext() {
        return getThreadResponseContext();
    }

    protected void clearContext(ThreadLocal<Map<String, Object>> context) {
        context.set(null);
    }

    public Binding getBinding() {
        return binding;
    }

    protected void populateResponseContext(MessageContext ctx) {

        Iterator<String> iter  = ctx.keySet().iterator();
        Map<String, Object> respCtx = getResponseContext();
        while (iter.hasNext()) {
            String obj = iter.next();
            if (MessageContext.Scope.APPLICATION.compareTo(ctx.getScope(obj)) == 0) {
                respCtx.put(obj, ctx.get(obj));
            }
        }
    }

    public EndpointReference getEndpointReference() {
        return builder.getEndpointReference();
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        return builder.getEndpointReference(clazz);
    }

    protected Map<String, Object> getThreadRequestContext() {
        WeakReference<Map<String, Object>> r = requestContext.get();
        if ((null == r) || (null == r.get())) {
            r = new WeakReference<Map<String, Object>>(new HashMap<String, Object>());
            requestContext.set(r);
        }
        return (r != null) ? r.get() : null;
    }

    protected Map<String, Object> getThreadResponseContext() {
        WeakReference<Map<String, Object>> r = responseContext.get();
        if ((null == r) || (null == r.get())) {
            r = new WeakReference<Map<String, Object>>(
                    new WrappedMessageContext(
                    new HashMap<String, Object>(), null, Scope.APPLICATION));
            responseContext.set(r);
        }
        return (r != null) ? r.get() : null;
    }

}
