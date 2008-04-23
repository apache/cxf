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

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.SimpleMethodDispatcher;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;

public class JAXWSMethodDispatcher extends SimpleMethodDispatcher {

    private static final Logger LOG = LogUtils.getL7dLogger(JAXWSMethodDispatcher.class);

    private JaxWsImplementorInfo implInfo;
    
    public JAXWSMethodDispatcher(JaxWsImplementorInfo implInfo) {
        this.implInfo = implInfo;
    }

    public void bind(OperationInfo o, Method... methods) {
        Method [] newMethods = new Method[methods.length];
        int i = 0;
        for (Method m : methods) {
            try {
                newMethods[i++] = getImplementationMethod(m);
            } catch (NoSuchMethodException e) {
                Class endpointClass = implInfo.getImplementorClass();
                Message msg = new Message("SEI_METHOD_NOT_FOUND", LOG, 
                                          m.getName(), endpointClass.getName());
                throw new ServiceConstructionException(msg, e);
            }
        }
        super.bind(o, newMethods);
    }

    public BindingOperationInfo getBindingOperation(Method method, Endpoint endpoint) {
        try {
            method = getImplementationMethod(method);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        return super.getBindingOperation(method, endpoint);
    }
    
    public Method getImplementationMethod(Method method) throws NoSuchMethodException {
        Class<?> endpointClass = implInfo.getImplementorClass();
        
        if (!endpointClass.isAssignableFrom(method.getDeclaringClass())) {
            try {
                method = endpointClass.getMethod(method.getName(), 
                                                 (Class[])method.getParameterTypes());
            } catch (SecurityException e) {
                throw new ServiceConstructionException(e);
            }
        }
        return method;
    }
    
}
