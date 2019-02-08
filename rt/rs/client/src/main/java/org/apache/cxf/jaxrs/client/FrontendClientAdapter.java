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
package org.apache.cxf.jaxrs.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;

/**
 * The adapter between JAX-RS and CXF Frontend Client, can be used in cases
 * when org.apache.cxf.endpoint.Client is expected by the runtime, example when
 * bus level features have to applied to a given JAX-RS client instance, etc
 */
class FrontendClientAdapter implements org.apache.cxf.endpoint.Client {
    private ClientConfiguration config;

    FrontendClientAdapter(ClientConfiguration config) {
        this.config = config;
    }

    @Override
    public List<Interceptor<? extends Message>> getInInterceptors() {
        return config.getInInterceptors();
    }

    @Override
    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return config.getOutInterceptors();
    }

    @Override
    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        return config.getInFaultInterceptors();
    }

    @Override
    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        return config.getOutFaultInterceptors();
    }

    @Override
    public Conduit getConduit() {
        return config.getConduit();
    }

    @Override
    public ConduitSelector getConduitSelector() {
        return config.getConduitSelector();
    }

    @Override
    public void setConduitSelector(ConduitSelector selector) {
        config.setConduitSelector(selector);
    }

    @Override
    public Bus getBus() {
        return config.getBus();
    }

    @Override
    public Endpoint getEndpoint() {
        return config.getEndpoint();
    }

    @Override
    public void destroy() {
        //complete, the actual JAX-RS Client will be closed via a different path
    }

    @Override
    public Map<String, Object> getRequestContext() {
        return config.getRequestContext();
    }

    @Override
    public Map<String, Object> getResponseContext() {
        return config.getResponseContext();
    }

    @Override
    public void setThreadLocalRequestContext(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isThreadLocalRequestContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExecutor(Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessage(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] invoke(String operationName, Object... params) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] invoke(QName operationName, Object... params) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] invokeWrapped(String operationName, Object... params) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] invokeWrapped(QName operationName, Object... params) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] invoke(BindingOperationInfo oi, Object... params) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] invoke(BindingOperationInfo oi, Object[] params, Map<String, Object> context)
        throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] invoke(BindingOperationInfo oi, Object[] params, Map<String, Object> context,
                           Exchange exchange) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invoke(ClientCallback callback, String operationName, Object... params) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invoke(ClientCallback callback, QName operationName, Object... params) throws Exception {
        throw new UnsupportedOperationException();

    }

    @Override
    public void invokeWrapped(ClientCallback callback, String operationName, Object... params)
        throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invokeWrapped(ClientCallback callback, QName operationName, Object... params)
        throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invoke(ClientCallback callback, BindingOperationInfo oi, Object... params) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invoke(ClientCallback callback, BindingOperationInfo oi, Object[] params,
                       Map<String, Object> context) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invoke(ClientCallback callback, BindingOperationInfo oi, Object[] params, Exchange exchange)
        throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invoke(ClientCallback callback, BindingOperationInfo oi, Object[] params,
                       Map<String, Object> context, Exchange exchange) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws Exception {
        destroy();
    }
}
