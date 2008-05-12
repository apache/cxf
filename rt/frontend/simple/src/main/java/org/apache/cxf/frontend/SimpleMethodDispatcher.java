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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;

public class SimpleMethodDispatcher implements MethodDispatcher {

    private Map<Method, Map<BindingInfo, BindingOperationInfo>> infoMap = 
        new ConcurrentHashMap<Method, Map<BindingInfo, BindingOperationInfo>>();
    private Map<OperationInfo, Method> opToMethod = 
        new ConcurrentHashMap<OperationInfo, Method>();
    private Map<Method, OperationInfo> methodToOp = 
        new ConcurrentHashMap<Method, OperationInfo>();

    public SimpleMethodDispatcher() {
        //complete
    }
    
    public void bind(OperationInfo o, Method... methods) {
        Method primary = methods[0];
        for (Method m : methods) {
            methodToOp.put(m, o);            
            
            Map<BindingInfo, BindingOperationInfo> biToBop 
                = new ConcurrentHashMap<BindingInfo, BindingOperationInfo>();
            infoMap.put(m, biToBop);
        }
        
        opToMethod.put(o, primary);
        
        if (o.isUnwrappedCapable()) {
            opToMethod.put(o.getUnwrappedOperation(), primary);
        }
    }

    public BindingOperationInfo getBindingOperation(Method method, Endpoint endpoint) {
        Map<BindingInfo, BindingOperationInfo> bops = infoMap.get(method);
        if (bops == null) {
            return null;
        }
        
        BindingOperationInfo bop = bops.get(endpoint.getEndpointInfo().getBinding());
        if (bop == null) {
            OperationInfo o = methodToOp.get(method);
            if (o == null) {
                return null;
            }
            
            BindingInfo b = endpoint.getEndpointInfo().getBinding();
            for (BindingOperationInfo bop2 : b.getOperations()) {
                if (bop2.getOperationInfo().equals(o)) {
                    bop2 = getRealOperation(o, bop2);
                    
                    bops.put(b, bop2);
                    return bop2;
                }
            }
        }
        return bop;
    }

    private BindingOperationInfo getRealOperation(OperationInfo o, BindingOperationInfo bop) {
        BindingOperationInfo unwrappedOp = bop.getUnwrappedOperation();
        if (unwrappedOp != null
            && unwrappedOp.getOperationInfo().equals(o.getUnwrappedOperation())) {
            bop = unwrappedOp;
        }
        return bop;
    }

    public Method getMethod(BindingOperationInfo op) {
        return opToMethod.get(op.getOperationInfo());
    }
    
    public Method getPrimaryMethod(OperationInfo op) {
        return opToMethod.get(op);
    }
}
