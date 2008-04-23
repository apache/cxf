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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;

public class JAXWSProviderMethodDispatcher implements MethodDispatcher {

    Method invoke;
    
    public JAXWSProviderMethodDispatcher(JaxWsImplementorInfo implInfo) {
        Type[] genericInterfaces = implInfo.getImplementorClass().getGenericInterfaces();
        ParameterizedType pt = (ParameterizedType)genericInterfaces[0];
        Class c = (Class)pt.getActualTypeArguments()[0];
        try {
            invoke = implInfo.getImplementorClass().getMethod("invoke", c);
        } catch (Exception e) {
            throw new ServiceConstructionException(e);
        }
    }

    public BindingOperationInfo getBindingOperation(Method m, Endpoint endpoint) {
        // TODO Auto-generated method stub
        return null;
    }

    public Method getMethod(BindingOperationInfo op) {
        return invoke;
    }

    public void bind(OperationInfo o, Method... methods) {
        // TODO Auto-generated method stub
    }

    
}
