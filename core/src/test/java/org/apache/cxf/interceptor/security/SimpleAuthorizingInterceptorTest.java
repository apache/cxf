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

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimpleAuthorizingInterceptorTest {

    protected Message message = new MessageImpl();
    private Method method;


    @Before
    public void setUp() throws Exception {
        method = TestService.class.getMethod("echo", new Class[]{});
        Exchange ex = setUpExchange();
        Service service = mock(Service.class);
        ex.put(Service.class, service);
        MethodDispatcher md = mock(MethodDispatcher.class);
        when(service.get(MethodDispatcher.class.getName())).thenReturn(md);

        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        ex.put(BindingOperationInfo.class, boi);
        when(md.getMethod(boi)).thenReturn(method);
    }

    protected Exchange setUpExchange() {
        message.put(SecurityContext.class, new TestSecurityContext());
        Exchange ex = new ExchangeImpl();
        message.setExchange(ex);
        return ex;
    }

    protected SimpleAuthorizingInterceptor createSimpleAuthorizingInterceptor() {
        return new SimpleAuthorizingInterceptor();
    }

    protected SimpleAuthorizingInterceptor createSimpleAuthorizingInterceptorWithDenyRoles(final String role) {
        return new SimpleAuthorizingInterceptor() {
            @Override
            public List<String> getDenyRoles(Method m) {
                return Collections.singletonList(role);
            }
        };
    }

    @Test(expected = AccessDeniedException.class)
    public void testNoSecurityContext() {
        message.put(SecurityContext.class, null);
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setAllowAnonymousUsers(false);
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testNoSecurityContextAnonymousUserRoles() {
        message.put(SecurityContext.class, null);
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("echo", "role1 testRole"));
        in.handleMessage(message);
    }
    @Test
    public void testNoSecurityContextAnonymousUserUnprotectedMethod() {
        message.put(SecurityContext.class, null);
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testIncompleteSecurityContext() {
        message.put(SecurityContext.class, new IncompleteSecurityContext());
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setAllowAnonymousUsers(false);
        in.handleMessage(message);
    }

    @Test
    public void testPermitWithNoRoles() {
        createSimpleAuthorizingInterceptor().handleMessage(message);
    }

    @Test
    public void testPermitWithMethodRoles() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("echo", "role1 testRole"));
        in.handleMessage(message);
    }

    @Test
    public void testPermitWithMethodRolesConfigurationOnly() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setCheckConfiguredRolesOnly(true);
        in.setUserRolesMap(Collections.singletonMap("testUser", "role1"));
        in.setMethodRolesMap(Collections.singletonMap("echo", "role1 role2"));
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testDenyWithMethodRolesConfigurationOnly() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setCheckConfiguredRolesOnly(true);
        in.setUserRolesMap(Collections.singletonMap("testUser", "role1"));
        in.setMethodRolesMap(Collections.singletonMap("echo", "role2 role3"));
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testEmptyRolesConfigurationOnly() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setCheckConfiguredRolesOnly(true);
        in.setMethodRolesMap(Collections.singletonMap("echo", "role1 role2"));
        in.handleMessage(message);
    }

    @Test
    public void testPermitAll() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("echo", "*"));
        in.handleMessage(message);
    }

    @Test
    public void testPermitWithClassRoles() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setGlobalRoles("role1 testRole");
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testDenyWithMethodRoles() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("echo", "role1 role2"));
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testDenyWithClassRoles() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptor();
        in.setGlobalRoles("role1 role2");
        in.handleMessage(message);
    }

    @Test
    public void testPermitWithDenyRoles() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptorWithDenyRoles("frogs");
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testDenyWithDenyRoles() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptorWithDenyRoles("testRole");
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testDenyAll() {
        SimpleAuthorizingInterceptor in = createSimpleAuthorizingInterceptorWithDenyRoles("*");
        in.handleMessage(message);
    }

    private static final class TestService {
        @SuppressWarnings("unused")
        public void echo() {

        }
    }

    private static final class IncompleteSecurityContext implements SecurityContext {

        public Principal getUserPrincipal() {
            return null;
        }

        public boolean isUserInRole(String role) {
            return false;
        }

    }

    private static final class TestSecurityContext implements SecurityContext {

        public Principal getUserPrincipal() {
            return new Principal() {
                public String getName() {
                    return "testUser";
                }
            };
        }

        public boolean isUserInRole(String role) {
            return "testRole".equals(role);
        }

    }
}
