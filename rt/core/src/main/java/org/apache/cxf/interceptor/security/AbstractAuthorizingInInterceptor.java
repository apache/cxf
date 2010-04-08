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
package org.apache.cxf.interceptor.security;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;

public abstract class AbstractAuthorizingInInterceptor extends AbstractPhaseInterceptor<Message> {

    public AbstractAuthorizingInInterceptor() {
        super(Phase.PRE_INVOKE);
    }
    
    public void handleMessage(Message message) throws Fault {
        SecurityContext sc = message.get(SecurityContext.class);
        if (sc == null) {
            return;
        }
        
        Method method = getTargetMethod(message);
        
        List<String> expectedRoles = getExpectedRoles(method);
        if (expectedRoles == null || expectedRoles.isEmpty()) {
            return;
        }
        
        for (String role : expectedRoles) {
            if (sc.isUserInRole(role)) {
                return;
            }
        }
        
        throw new AccessDeniedException("Unauthorized");
    }
    
    private Method getTargetMethod(Message m) {
        BindingOperationInfo bop = m.getExchange().get(BindingOperationInfo.class);
        MethodDispatcher md = (MethodDispatcher) 
            m.getExchange().get(Service.class).get(MethodDispatcher.class.getName());
        return md.getMethod(bop);
    }

    protected abstract List<String> getExpectedRoles(Method method);
}
