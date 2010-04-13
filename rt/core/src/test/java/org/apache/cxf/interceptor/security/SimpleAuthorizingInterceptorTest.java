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
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.easymock.classextension.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleAuthorizingInterceptorTest extends Assert {

    private Method method;
    private Message message = new MessageImpl();
    
    @Before
    public void setUp() throws Exception {
        method = TestService.class.getMethod("echo", new Class[]{});
        message.put(SecurityContext.class, new TestSecurityContext());
        Exchange ex = new ExchangeImpl();
        message.setExchange(ex);
        
        Service service = EasyMock.createMock(Service.class);
        ex.put(Service.class, service);
        MethodDispatcher md = EasyMock.createMock(MethodDispatcher.class);
        service.get(MethodDispatcher.class.getName());
        EasyMock.expectLastCall().andReturn(md);
        
        BindingOperationInfo boi = EasyMock.createMock(BindingOperationInfo.class);
        ex.put(BindingOperationInfo.class, boi);
        md.getMethod(boi);
        EasyMock.expectLastCall().andReturn(method);
        EasyMock.replay(service, md);
    }
    
    @Test
    public void testPermitWithNoRoles() {
        new SimpleAuthorizingInterceptor().handleMessage(message);    
    }
    
    @Test
    public void testPermitWithMethodRoles() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("echo", "role1 testRole"));
        in.handleMessage(message);    
    }
    
    @Test
    public void testPermitAll() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("echo", "*"));
        in.handleMessage(message);    
    }
    
    @Test
    public void testPermitWithClassRoles() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor();
        in.setGlobalRoles("role1 testRole");
        in.handleMessage(message);    
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testDenyWithMethodRoles() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("echo", "role1 role2"));
        in.handleMessage(message);    
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testDenyWithClassRoles() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor();
        in.setGlobalRoles("role1 role2");
        in.handleMessage(message);    
    }
    
    @Test
    public void testPermitWithDenyRoles() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor() {
            @Override
            public List<String> getDenyRoles(Method m) {
                return Collections.singletonList("frogs");
            }
           
        };
        in.handleMessage(message);    
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testDenyWithDenyRoles() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor() {
            @Override
            public List<String> getDenyRoles(Method m) {
                return Collections.singletonList("testRole");
            }
           
        };
        in.handleMessage(message);    
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testDenyAll() {
        SimpleAuthorizingInterceptor in = new SimpleAuthorizingInterceptor() {
            @Override
            public List<String> getDenyRoles(Method m) {
                return Collections.singletonList("*");
            }
           
        };
        in.handleMessage(message);    
    }
    
    private static class TestService {
        public void echo() {
            
        }
    }
    
    private static class TestSecurityContext implements SecurityContext {

        public Principal getUserPrincipal() {
            return null;
        }

        public boolean isUserInRole(String role) {
            return "testRole".equals(role);
        }
        
    }
}
