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

package org.apache.cxf.frontend;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.BindingOperationInfo;

public class ClientProxy implements InvocationHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(ClientProxy.class);

    protected Client client;
    private Endpoint endpoint;


    public ClientProxy(Client c) {
        endpoint = c.getEndpoint();
        client = c;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        MethodDispatcher dispatcher = (MethodDispatcher)endpoint.getService().get(
                                                                                  MethodDispatcher.class
                                                                                      .getName());
        BindingOperationInfo oi = dispatcher.getBindingOperation(method, endpoint);
        if (oi == null) {
            // check for method on BindingProvider and Object
            if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(this);
            }

            throw new Fault(new Message("NO_OPERATION_INFO", LOG, method.getName()));
        }

        Object[] params = args;
        if (null == params) {
            params = new Object[0];
        }

        return invokeSync(method, oi, params);
    }

    public Object invokeSync(Method method, BindingOperationInfo oi, Object[] params)
        throws Exception {
        Object rawRet[] = client.invoke(oi, params);

        if (rawRet != null && rawRet.length > 0) {
            return rawRet[0];
        } else {
            return null;
        }
    }
    public Map<String, Object> getRequestContext() {
        return client.getRequestContext();
    }
    public Map<String, Object> getResponseContext() {
        return client.getResponseContext();
    }

    public Client getClient() {
        return client;
    }

    public static Client getClient(Object o) {
        return ((ClientProxy)Proxy.getInvocationHandler(o)).getClient();
    }
}
