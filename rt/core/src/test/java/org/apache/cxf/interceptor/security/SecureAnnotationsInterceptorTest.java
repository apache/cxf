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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Method;
import java.security.Principal;

import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class SecureAnnotationsInterceptorTest extends Assert {

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
        new SecureAnnotationsInterceptor().handleMessage(message);    
    }
    
    @Test
    public void testPermitWithMethodRoles() {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        in.setSecuredObject(new TestService());
        in.handleMessage(message);    
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testAccessDeniedMethodRoles() {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        in.setSecuredObject(new TestService2());
        in.handleMessage(message);    
    }
    
    
    @Retention (RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD })
    public @interface SecureRolesAllowed {
        String[] value();
    }
    
    private static class TestService {
        @SuppressWarnings("unused")
        @SecureRolesAllowed("testRole")
        public void echo() {
        }
    }
    
    private static class TestService2 {
        @SuppressWarnings("unused")
        @SecureRolesAllowed("baz")
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
