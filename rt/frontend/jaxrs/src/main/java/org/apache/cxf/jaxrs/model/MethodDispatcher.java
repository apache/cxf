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
package org.apache.cxf.jaxrs.model;

import java.lang.reflect.Method;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class MethodDispatcher {
    private Map<OperationResourceInfo, Method> oriToMethod = 
        new ConcurrentHashMap<OperationResourceInfo, Method>();
    private Map<Method, OperationResourceInfo> methodToOri = 
        new ConcurrentHashMap<Method, OperationResourceInfo>();

    public void bind(OperationResourceInfo o, Method... methods) {
        Method primary = methods[0];

        for (Method m : methods) {
            methodToOri.put(m, o);
        }

        oriToMethod.put(o, primary);
    }

    public OperationResourceInfo getOperationResourceInfo(Method method) {
        return methodToOri.get(method);
    }

    public Set<OperationResourceInfo> getOperationResourceInfos() {
        return oriToMethod.keySet();
    }

    public Method getMethod(OperationResourceInfo op) {
        return oriToMethod.get(op);
    }
}
